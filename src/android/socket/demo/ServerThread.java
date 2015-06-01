package android.socket.demo;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.socket.demo.Connection.ConnectionInternalListener;
import android.socket.demo.Connection.ConnectionInternalReceiver;
import android.util.Log;

@SuppressLint("UseSparseArrays")
public class ServerThread extends SocketThread {
    private final static String TAG = "WJZ";
    private HashMap<Integer, Connection> mClients;
    private ServerSocket mServerSocket;
    private Handler mHandler;
    private ConnectionInternalListener mListener;
    private ConnectionInternalReceiver mReceiver;
    private boolean mAlive = false;
    protected void onLooperPrepared() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle data = msg.getData();
                switch(msg.what) {
                    case SocketThread.MSG_WHAT_SEND_MSG:
                        handleSendMessage(data.getInt(SocketThread.MSG_BUNDLE_KEY_TAG),
                                data.getString(SocketThread.MSG_BUNDLE_SEND_MSG));
                        break;
                    case SocketThread.MSG_WHAT_EXIT:
                        handleExit();
                        break;
                }
            }
        };
        mAlive = true;
        startListen();
    }

    // constructor
    public ServerThread(String name, ConnectionInternalListener listener,
            ConnectionInternalReceiver receiver) {
        super(name);
        mListener = listener;
        mReceiver = receiver;
    }

    @Override
    public void sendMessage(int tag, String content) {
        Bundle data = new Bundle();
        data.putInt(SocketThread.MSG_BUNDLE_KEY_TAG, tag);
        data.putString(SocketThread.MSG_BUNDLE_SEND_MSG, content);
        Message msg = mHandler.obtainMessage(SocketThread.MSG_WHAT_SEND_MSG);
        msg.setData(data);
        msg.sendToTarget();
        Log.v(TAG, "ServerThread.sendMessage: " + content + ", tag = " +  tag);
    }

    @Override
    synchronized public void exit() {
        if (mAlive) {
            Message msg = mHandler.obtainMessage(SocketThread.MSG_WHAT_EXIT);
            msg.sendToTarget();
        }
    }

    private void startListen() {
        if (mServerSocket != null) {
            Log.v(TAG, "Server socket has started!");
            handleExit();
            return;
        }
        // start a new thread to listen
        new Thread() {
            @Override
            public void run() {
                 try {
                     Log.v(TAG, "Server is listening");
                     mServerSocket = new ServerSocket(SocketUtils.PORT);
                     while(true) {
                        Socket socket = mServerSocket.accept();
                        Log.v(TAG, "A client is connected: " + socket.getInetAddress().toString());
                        new Connection(ServerThread.this, socket, mListener, mReceiver);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    Log.v(TAG, "Listening thread eixt!");
                    exit();
                }
            }
        }.start();
    }

    synchronized private void handleExit() {
        if (mAlive) {
            Log.v(TAG, "ServerThread exit!");
            closeObject(mServerSocket);
            mServerSocket = null;
            getLooper().quit();
            ConnectionManager.getManager().disconnectServerConnections();
        }
    }

    private void handleSendMessage(int tag, String msg) {
        Log.v(TAG, "ServerThread.handleSendMessage: " + msg + ", tag = " +  tag);
        byte[] buffer = msg.getBytes();
        Connection conn = ConnectionManager.getManager().mConnections.get(tag);
        if (conn != null) {
            Socket socket = conn.mSocket;
            if (socket != null) {
                try {
                    OutputStream out = socket.getOutputStream();
                    out.write(buffer);
                } catch (IOException e) {
                    Log.v(TAG, "Failed to send to client socket : " + socket.getInetAddress().toString());
                    conn.connectionInterrupted();
                }
            }
        }
    }
}
