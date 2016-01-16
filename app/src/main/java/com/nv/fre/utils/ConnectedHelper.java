package com.nv.fre.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by nover on 2016/1/16 0016.
 */
public class ConnectedHelper {
    private BroadcastReceiver connectedReceiver;
    public void registerConnectedCheck(Context context, final Callback callback) {
        connectedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent == null || intent.getAction() == null || callback == null || !callback.canRun()) {
                    return;
                }
                boolean connected = false;
                //获得网络连接服务
                ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                // State state = connManager.getActiveNetworkInfo().getState();
                // 获取WIFI网络连接状态
                NetworkInfo.State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
                // 判断是否正在使用WIFI网络
                if (NetworkInfo.State.CONNECTED == state) {
                    connected = true;
                }
                // 获取GPRS网络连接状态
                state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
                // 判断是否正在使用GPRS网络
                if (NetworkInfo.State.CONNECTED == state){
                    connected = true;
                }
                if(connected) {
                    callback.onConnected();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(connectedReceiver, intentFilter);
    }

    public void unregisterConnectedCheck(Context context) {
        if(connectedReceiver != null) {
            context.unregisterReceiver(connectedReceiver);
            connectedReceiver = null;
        }
    }

    public static abstract class Callback {
        public boolean canRun() { return true; }
        public abstract void onConnected();
    }
}
