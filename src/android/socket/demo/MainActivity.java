
package android.socket.demo;

import java.io.InputStream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.socket.demo.ConnectionManager.ConnectionListener;
import android.socket.demo.ConnectionManager.ConnectionReceiver;

public class MainActivity extends Activity implements OnClickListener {
    private static final String TAG = "WJZ";
    private static final String KEY_FROM = "from";
    private static final String KEY_TAG = "tag";
    private static final String KEY_MSG = "message";
    private final static long MIN_INTERVAL = 2000;
    // Server UI controls
    private TextView mLocalIPTv;
    private Button mServerStartBtn;
    private TextView mServerReceiveTv;
    private TextView mServerSendTv;
    private Button mServerSendBtn;
    // Client UI controls
    private TextView mClientConnectIPTv;
    private Button mClientConnectBtn;
    private TextView mClientReceiveTv;
    private TextView mClientSendTv;
    private Button mClientSendBtn;
    
    private ConnectivityManager mConnectivityManager;
    private WifiManager mWifiManager;
    private boolean mEnable;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private long mCurrentTime;
    private ConnectionManager mConnectionManager;
    private Handler mHandler;
    private int mTag = SocketUtils.TAG_INVALID;
    private int mServerTag = SocketUtils.TAG_INVALID;
    private int mClientTag = SocketUtils.TAG_INVALID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        Log.v(TAG, "MainActivity.onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "MainActivity.onResume");
        registerReceiver(mReceiver,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "MainActivity.onPause");
        unregisterReceiver(mReceiver);
    }
    private void init () {
        // get ui
        // Server UI controls
        mLocalIPTv = (TextView) findViewById(R.id.local_ip_tv);
        mServerStartBtn = (Button) findViewById(R.id.server_btn);
        mServerReceiveTv = (TextView) findViewById(R.id.server_receive_msg_tv);
        mServerSendTv = (TextView) findViewById(R.id.server_send_edit);
        mServerSendBtn = (Button) findViewById(R.id.server_send_btn);
        mServerStartBtn.setOnClickListener(this);
        mServerSendBtn.setOnClickListener(this);
        // Client UI controls
        mClientConnectIPTv = (TextView) findViewById(R.id.client_connect_ip_edit);
        mClientConnectBtn = (Button) findViewById(R.id.client_btn);
        mClientReceiveTv = (TextView) findViewById(R.id.client_receive_msg_tv);
        mClientSendTv = (TextView) findViewById(R.id.client_send_edit);
        mClientSendBtn = (Button) findViewById(R.id.client_send_btn);
        mClientConnectBtn.setOnClickListener(this);
        mClientSendBtn.setOnClickListener(this);

        // set network listener
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mReceiver = new NetworkReceiver(this);
        registerReceiver(mReceiver, mIntentFilter);
        // set wifi
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        // update ui
        updateNetworkStatus();
        // set up connection listener and receiver
        mConnectionManager = ConnectionManager.getManager();
        mConnectionManager.addConnectionListener(new ConnectionListener() {
            @Override
            public void onConnectionChanged(boolean connected, int tag) {
                if (connected) {
                    Log.v(TAG, "A new connection established: tag = " + tag);
                    int type = mConnectionManager.getConnectionType(tag);
                    if (type == SocketUtils.CONNECTION_TYPE_SERVER) {
                        mServerTag = tag;
                    } else if (type == SocketUtils.CONNECTION_TYPE_CLIENT) {
                        mClientTag = tag;
                    }
                } else {
                    Log.v(TAG, "A connection closed: tag = " + tag);
                }
            }
        });
        mConnectionManager.addConnectionReceiver(new ConnectionReceiver() {
            @Override
            public void onMessageReceive(int tag, String msg) {
                Log.v(TAG, "Receive message: msg = " + msg + ", tag = " + tag);
                Message message = mHandler.obtainMessage();
                Bundle data = new Bundle();
                data.putInt(KEY_TAG, tag);
                data.putString(KEY_MSG, msg);
                message.setData(data);
                message.sendToTarget();
            }

            @Override
            public void onFileReceive(int tag, InputStream in) {
                // TODO Auto-generated method stub
            }
        });

        // set up handler
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle data = msg.getData();
                int tag = data.getInt(KEY_TAG);
                String content = data.getString(KEY_MSG);
                if (tag == mServerTag) {
                    mServerReceiveTv.setText(content);
                } else if(tag == mClientTag) {
                    mClientReceiveTv.setText(content);
                }
            }
        };
    }

    @Override
    public void onClick(View view) {
        long currentTime = SystemClock.uptimeMillis();
        if (mCurrentTime != 0 && (currentTime - mCurrentTime) < MIN_INTERVAL) {
            return;
        }
        mCurrentTime = currentTime;
        if (!mEnable) {
            Toast.makeText(this, "No wifi connection", Toast.LENGTH_LONG).show();
            return;
        }
        if (view == mServerStartBtn) { // start server
            mConnectionManager.startServer();
        } else if (view == mClientConnectBtn) { // connect to server
            // check ip and port
            String ipString = mClientConnectIPTv.getText().toString();
            if (TextUtils.isEmpty(ipString)) {
                toast("Ip address cannot be empty!");
                return;
            }
            int ip = stringToIp(ipString);
            if (ip == 0) {
                toast("Invalid ip address:" + ipString);
                return;
            }

            mConnectionManager.startClient(ip);
        } else if (view == mServerSendBtn) { // server send msg to client
            if (mServerTag == SocketUtils.TAG_INVALID) {
                toast("Server has no client！");
                return;
            }
            String msgString = mServerSendTv.getText().toString();
            if (TextUtils.isEmpty(msgString)) {
                toast("Cannot send empty message!");
                return;
            }
            mConnectionManager.sendMessage(mServerTag, msgString);
        } else if (view == mClientSendBtn) {
            if (mClientTag == SocketUtils.TAG_INVALID) {
                toast("Client has not connected to a server！");
                return;
            }
            String msgString = mClientSendTv.getText().toString();
            if (TextUtils.isEmpty(msgString)) {
                toast("Cannot send empty message!");
                return;
            }
            mConnectionManager.sendMessage(mClientTag, msgString);
        }
    }

    private int stringToIp(String ipString) {
        String[] ipStrings = ipString.split("\\.");
        int ip = 0;
        Log.v(TAG, "IP segments length = " + ipStrings.length);
        if (ipStrings != null && ipStrings.length == 4) {
            for (int i = 0; i < 4; i++) {
                String str = ipStrings[i];
                Log.v(TAG, "Parsing ip segment: " + str);
                if (!TextUtils.isDigitsOnly(str)) {
                    return 0;
                }
                try {
                    int temp = Integer.valueOf(str);
                    if (temp < 0 || temp > 255) {
                        return 0;
                    }
                    int shift = 8 * i;
                    ip = ip + (temp << shift);
                } catch (Exception e) {
                    return 0;
                }
            }
        }
        Log.v(TAG, "server's ip is " + Integer.toHexString(ip));
        return ip;
    }

    public void networkChanged() {
        updateNetworkStatus();
    }

    private void updateNetworkStatus() {
        // NetworkInfo mobileInfo = mManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        // NetworkInfo activeInfo = mManager.getActiveNetworkInfo();
        NetworkInfo network = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        String text = "No wifi connection";
        if (network != null) {
            mEnable = network.isConnected();
            if (mEnable) {
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                int ipAddress = wifiInfo.getIpAddress();
                text = intToIp(ipAddress);
            }
        }
        mLocalIPTv.setText(text);
        Log.v(TAG, "network changed: enable = " + mEnable);
    }

    private String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
