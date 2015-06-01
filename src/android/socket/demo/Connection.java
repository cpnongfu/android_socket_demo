package android.socket.demo;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import android.util.Log;

public class Connection {
    private final static String TAG = "WJZ";
    // the connection type, as server or as client

    // the connection status
    public final static int STATUS_CONNECTED = 0;
    public final static int STATUS_DISCONNECTED = 1;
    SocketThread mSendThread;
    ReceiverThread mReceiverThread;
    Socket mSocket;
    byte[] mIpAddress;
    private ConnectionInternalReceiver mReceiver;
    private ConnectionInternalListener mListener;
    private boolean mConnected = false;
    int mTag = -1;
    int mConnectionType = SocketUtils.CONNECTION_TYPE_UNKNOWN;
    // To notify connection manager that connection changed
    static interface ConnectionInternalListener {
        public void onConnectionChanged(Connection conn, boolean connected);
    }
    // To notify connection manager that something received
    static interface ConnectionInternalReceiver {
        public void onMessageReceive(Connection conn, String msg);
        public void onFileReceive(Connection conn, InputStream in);
    }
    // used for client
    Connection(byte[] ip, ConnectionInternalListener listener,
            ConnectionInternalReceiver receiver) {
        mIpAddress = ip;
        mListener = listener;
        mReceiver = receiver;
        mConnectionType = SocketUtils.CONNECTION_TYPE_CLIENT;
    }
    // used for server
    Connection(ServerThread thread, Socket socket, ConnectionInternalListener listener,
            ConnectionInternalReceiver receiver) {
        mSendThread = thread;
        mSocket = socket;
        mTag = mSocket.hashCode();
        mIpAddress = socket.getInetAddress().getAddress();
        mListener = listener;
        mReceiver = receiver;
        mConnectionType = SocketUtils.CONNECTION_TYPE_SERVER;
        mConnected = true;
        // notify connection manager socket is connected
        if (mListener != null) {
            mListener.onConnectionChanged(this, true);
        }
        // start a new thread to receive message
        mReceiverThread = new ReceiverThread(this, mSocket);
        mReceiverThread.start();
    }

    // only client need to invoke this function
    void connect() {
        if (mConnectionType == SocketUtils.CONNECTION_TYPE_SERVER) {
            Log.v(TAG, "Server connection cannot be established by user!");
        }
        if (!mConnected) {
            mSendThread = new ClientThread("Client connection", this);
            mSendThread.start();
        } else {
            Log.v(TAG, "Failed to connect, reason: connection has already been established");
        }
    }

    // only client need to invoke this function
    void disconnect() {
        connectionInterrupted();
    }

    // only client need to invoke this
    void clientConnectionEstablished(Socket socket) {
        mSocket = socket;
        mTag = mSocket.hashCode();
        mConnected = true;
        if (mListener != null) {
            mListener.onConnectionChanged(this, true);
        }
        // start a new thread to receive message
        mReceiverThread = new ReceiverThread(this, mSocket);
        mReceiverThread.start();
    }

    synchronized void connectionInterrupted() {
        Log.v(TAG, "connection is interrupted!");
        if (mConnected) {
            mConnected = false;
            // close socket, and this will interrupt receiver thread
            if (mSocket == null) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    Log.v(TAG, "Failed to close socket: " + e.toString());
                } finally {
                    mSocket = null;
                }
            }
            mReceiverThread = null;
            mSendThread = null;
            if (mListener != null) {
                mListener.onConnectionChanged(this, false);
            }
        }
    }

    void receiveMessage(String msg) {
        if (mReceiver != null) {
            mReceiver.onMessageReceive(this, msg);
        }
    }

    void sendMessage(String msg) {
        mSendThread.sendMessage(mTag, msg);
    }
}
