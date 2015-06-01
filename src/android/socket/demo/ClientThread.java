package android.socket.demo;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ClientThread extends SocketThread {
    private static final String TAG = "WJZ";
    private static final int TIMEOUT = 5000; //ms
    private Handler mHandler;
    private Socket mSocket;
    private OutputStream mOut;
    private Connection mConnection;
    private boolean mAlive = false;
    private Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            handleExit();
        }
    };

    @Override
    protected void onLooperPrepared() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.v(TAG, "ClientThread.Handler: receive msg: " + msg.what);
                switch (msg.what) {
                    case SocketThread.MSG_WHAT_SEND_MSG:
                        String content = msg.getData().getString(SocketThread.MSG_BUNDLE_SEND_MSG);
                        handleSendMessage(content);
                        break;
                    case SocketThread.MSG_WHAT_EXIT:
                        handleExit();
                        break;
                }
            }
        };
        mAlive = true;
        mHandler.postDelayed(mTimeoutRunnable, TIMEOUT);
        connect();
    }

    // constructor
    public ClientThread(String name, Connection conn) {
        super(name);
        mConnection = conn;
    }

    @Override
    public void sendMessage(int tag, String content) {
        Bundle data = new Bundle();
        data.putString(SocketThread.MSG_BUNDLE_SEND_MSG, content);
        Message msg = mHandler.obtainMessage(SocketThread.MSG_WHAT_SEND_MSG);
        msg.setData(data);
        msg.sendToTarget();
    }

    private void connect() {
        Log.v(TAG, "Begin to connect " + mConnection.mIpAddress);
        try {
            InetAddress ipAddress = InetAddress.getByAddress(mConnection.mIpAddress);
            mSocket = new Socket(ipAddress, SocketUtils.PORT);
            mOut = mSocket.getOutputStream();
            //mHandler.postDelayed(mTimeoutRunnable, TIMEOUT);
            mHandler.removeCallbacks(mTimeoutRunnable);
            mConnection.clientConnectionEstablished(mSocket);
        } catch (IOException e) {
            e.printStackTrace();
            Log.v(TAG, "Failed to get socket:" + e.toString());
            handleExit();
        }
    }

    private void handleExit() {
        if (mAlive) {
            mAlive = false;
            Log.v(TAG, "Client ends!");
            closeObject(mOut);
            mOut = null;
            mConnection.connectionInterrupted();
            this.getLooper().quit();
        }
    }

    private void handleSendMessage(String msg) {
        byte[] buffer = msg.getBytes();
        if (mOut != null) {
            try {
                mOut.write(buffer);
            } catch (IOException e) {
                Log.v(TAG, "ClientThread.sendMessage : " + e.toString());
                mConnection.connectionInterrupted();
            }
        }
    }

    @Override
    synchronized public void exit() {
        if (mAlive) {
            Message msg = mHandler.obtainMessage(SocketThread.MSG_WHAT_EXIT);
            msg.sendToTarget();
        }
    }
}
