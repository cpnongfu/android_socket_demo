package android.socket.demo;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import android.util.Log;

public class ReceiverThread extends Thread{
    private final static String TAG = "WJZ";
    private Socket mSocket;
    private InputStream mIn;
    private byte[] mBuffer = new byte[1024];
    private int mTag;
    private Connection mConnection;

    // constructor
    public ReceiverThread(Connection conn, Socket socket) {
        mConnection = conn;
        mSocket = socket;
    }

    @Override
    public void run() {
        int bytesRead;
        try {
            mIn = mSocket.getInputStream();
            while(true) {
                // wait and receive msg
                bytesRead = mIn.read(mBuffer, 0, mBuffer.length);
                if(bytesRead == -1) {
                    Log.v(TAG, "Failed to receive msg from : " + mSocket.getInetAddress().toString());
                    break;
                }
                // send message to UI
                final String content = new String(mBuffer, 0, bytesRead);
                mConnection.receiveMessage(content);
            }
        } catch (IOException e) {
            Log.v(TAG, "IO error occurs during receiving: " + e.toString());
        } catch(Exception e) {
            Log.v(TAG, "Unknown error occurs during receiving: " + e.toString());
        } finally {
            Log.v(TAG, "ReceiverThread exited!!");
            SocketThread.closeObject(mIn);
            mIn = null;
            mConnection.connectionInterrupted();
        }
    }
}
