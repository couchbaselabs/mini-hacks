package com.couchbase.gcmexample;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Foreground foreground = Foreground.get(getApplicationContext());
                if (foreground.isBackground()) {
                    Log.d(Application.TAG, "From: " + remoteMessage.getFrom());
                    Toast.makeText(getApplicationContext(), "Server ping - sync down!", Toast.LENGTH_LONG).show();

                    BackgroundSync backgroundSync = new BackgroundSync();
                    backgroundSync.execute();
                }
            }
        }, 2000);
    }
}