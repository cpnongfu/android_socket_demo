package android.socket.demo;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

import android.R.integer;
import android.os.HandlerThread;

public abstract class SocketThread extends HandlerThread {
    public final static int MSG_WHAT_START = 0;
    public final static int MSG_WHAT_SEND_MSG = 1;
    public final static int MSG_WHAT_EXIT = 2;
    public final static String MSG_BUNDLE_KEY_IP = "ip";
    public final static String MSG_BUNDLE_KEY_PORT = "port";
    public final static String MSG_BUNDLE_KEY_TAG = "tag";
    public final static String MSG_BUNDLE_SEND_MSG = "send_msg";
    public final static String MSG_BUNDLE_KEY_LISTENER = "listener";
    public final static int TAG_EXIT_THREAD = 0;
    public final static int TAG_INVALID = 0;
    public final static int FROM_SERVER = 0;
    public final static int FROM_CLIENT = 1;
    public final static int REASON_SUCCESS = 0;
    public final static int REASON_FAILED = 1;
    Socket mSocket;
    // constructor
    public SocketThread(String name) {
        super(name);
    }

    public abstract void sendMessage(int tag, String msg);
    public abstract void exit();
    public boolean isConnected() {
        return false;
    }

    // close object;
    static void closeObject(Closeable obj) {
        if (obj != null) {
            try {
                obj.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
