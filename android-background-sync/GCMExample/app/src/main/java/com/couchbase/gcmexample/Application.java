package com.couchbase.gcmexample;

import android.util.Log;

public class Application extends android.app.Application {

    public static String TAG = "GCM Example";

    @Override
    public void onCreate() {
        super.onCreate();

        final SyncManager syncManager = SyncManager.getSharedInstance(getApplicationContext());

        Foreground foreground = Foreground.init(this);
        foreground.addListener(new Foreground.Listener() {
            @Override
            public void onBecameForeground() {
                Log.d(Application.TAG, "Foreground...");
                syncManager.startPull(true);
                syncManager.startPush();
            }

            @Override
            public void onBecameBackground() {
                Log.d(Application.TAG, "Background...");
                syncManager.stopReplications();
            }
        });
    }

}
