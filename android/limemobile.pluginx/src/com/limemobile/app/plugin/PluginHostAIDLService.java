package com.limemobile.app.plugin;

import com.limemobile.app.plugin.aidl.IPluginHost;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PluginHostAIDLService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IPluginHost.Stub mBinder = new IPluginHost.Stub() {
    };
}
