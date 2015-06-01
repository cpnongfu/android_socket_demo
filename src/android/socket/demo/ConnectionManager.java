package android.socket.demo;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import android.annotation.SuppressLint;

import android.socket.demo.Connection.ConnectionInternalListener;
import android.socket.demo.Connection.ConnectionInternalReceiver;
import android.util.Log;

@SuppressLint("UseSparseArrays")
public class ConnectionManager {
    private final static String TAG = "WJZ";
    private ServerThread mServer;
    private ConnectionInternalReceiver mReceiver;
    private ConnectionInternalListener mListener;
    private ArrayList<ConnectionListener> mListeners;
    private ArrayList<ConnectionReceiver> mReceivers;
    HashMap<Integer,Connection> mConnections;
    private boolean mServerStarted = false;
    public interface ConnectionListener {
        public void onConnectionChanged(boolean connected, int tag);
    }
    public interface ConnectionReceiver {
        public void onMessageReceive(int tag, String msg);
        public void onFileReceive(int tag, InputStream in);
    }

    private static ConnectionManager sSingleManager;

    public static ConnectionManager getManager() {
        if (sSingleManager == null) {
            sSingleManager = new ConnectionManager();
        }
        return sSingleManager;
    }

    private ConnectionManager() {
        mListener = new ConnectionInternalListener() {
            @Override
            public void onConnectionChanged(Connection conn, boolean connected) {
                if (connected) {
                    Log.v(TAG, "Add a new connection");
                    mConnections.put(conn.mTag, conn);
                } else {
                    Log.v(TAG, "Remove a connection");
                    mConnections.remove(conn.mTag);
                }
                for (ConnectionListener listener : mListeners) {
                    listener.onConnectionChanged(connected, conn.mTag);
                }
            }
        };
        mReceiver = new ConnectionInternalReceiver() {
            @Override
            public void onMessageReceive(Connection conn, String msg) {
                for (ConnectionReceiver receiver : mReceivers) {
                    receiver.onMessageReceive(conn.mTag, msg);
                }
            }
            @Override
            public void onFileReceive(Connection conn, InputStream in) {
                // empty
            }
        };
        mListeners = new ArrayList<ConnectionListener>();
        mReceivers = new ArrayList<ConnectionReceiver>();
        mConnections = new HashMap<Integer, Connection>();
    }

    public void startServer() {
        if (!mServerStarted) {
            mServer = new ServerThread("ServerThread", mListener, mReceiver);
            mServer.start();
            mServerStarted  = true;
        }
    }

    public void addConnection(Connection conn) {
        if (conn != null) {
            mConnections.put(conn.mTag, conn);
        }
    }

    public void removeConnection(Connection conn) {
        if (conn != null) {
            mConnections.remove(conn.mTag);
        }
    }

    public void startClient(int ip) {
        Connection conn = new Connection(intToIpBytes(ip), mListener, mReceiver);
        conn.connect();
    }

    private byte[] intToIpBytes(int ip) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (ip & 0xFF);
        bytes[1] = (byte) ((ip >> 8) & 0xFF);
        bytes[2] = (byte) ((ip >> 16) & 0xFF);
        bytes[3] = (byte) ((ip >> 24) & 0xFF);
        return bytes;
    }

    public void stopServer() {
        mServer.exit();
    }

    public void closeConnection(int tag) {
        Connection conn = mConnections.get(tag);
        if (conn != null) {
            conn.disconnect();
        }
    }

    public void sendMessage(int tag, String msg) {
        Connection conn = mConnections.get(tag);
        if (conn != null) {
            conn.sendMessage(msg);
        }
    }

    public void addConnectionListener(ConnectionListener listener) {
        mListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        mListeners.remove(listener);
    }

    public void addConnectionReceiver(ConnectionReceiver receiver) {
        mReceivers.add(receiver);
    }

    public void removeConnectionListener(ConnectionReceiver receiver) {
        mReceivers.remove(receiver);
    }

    public void disconnectServerConnections() {
        if (mServerStarted) {
            mServer = null;
            mServerStarted = false;
        }
    }

    public int getConnectionType(int tag) {
        Connection conn = mConnections.get(tag);
        if (conn != null) {
            return conn.mConnectionType;
        }
        return SocketUtils.CONNECTION_TYPE_UNKNOWN;
    }
}
