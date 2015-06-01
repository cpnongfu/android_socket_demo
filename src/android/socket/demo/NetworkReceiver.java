package android.socket.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkReceiver extends BroadcastReceiver {
    private static final String TAG = "WJZ";
    private MainActivity mActivity;
    public NetworkReceiver(MainActivity activity) {
        mActivity = activity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "SocketReceiver.onReceive");
        mActivity.networkChanged();
    }
}
