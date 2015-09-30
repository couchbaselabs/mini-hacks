package com.couchbase.ratingapp;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Reducer;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.listener.Credentials;
import com.couchbase.lite.listener.LiteListener;
import com.couchbase.lite.replicator.Replication;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Acme.Serve.Main;

public class StorageManager {
    static private String stringURL = "http://178.62.162.87:4984/ratingapp";
    static public String UNIQUE_RATINGS_VIEW = "byUniqueRating";
    static public String USER_RATINGS_VIEW = "byUserRating";
    int LISTENER_PORT = 55000;

    Manager manager;
    Database database;

    Replication syncGatewayPull;
    Replication syncGatewayPush;

    Replication peerPull;
    Replication peerPush;

    URL url;

    public StorageManager(Context context) {

        try {
            /** Enable logging in the application for all tags */
            Manager.enableLogging("RatingApp", Log.VERBOSE);
            manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            database = manager.getDatabase("ratingapp");
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        registerViews();

        continuousReplications();

        startListener();
    }

    /**
     * Register the views when the database is fist opened.
     */
    private void registerViews() {
        View ratingsView = database.getView(UNIQUE_RATINGS_VIEW);
        ratingsView.setMapReduce(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (document.get("type").equals("unique")) {
                    emitter.emit(document.get("rating").toString(), null);
                }
            }
        }, new Reducer() {
            @Override
            public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
                return new Integer(values.size());
            }
        }, "16");

        View userRatingsView = database.getView(USER_RATINGS_VIEW);
        userRatingsView.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (document.get("type").equals("conflict")) {
                    emitter.emit((String) document.get("_id"), null);
                }
            }
        }, "4");
    }

    /**
     * Start push/pull replications with Sync Gateway.
     */
    private void continuousReplications() {
        try {
            url = new URL(stringURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        syncGatewayPush = database.createPushReplication(url);
        syncGatewayPush.setContinuous(true);
        syncGatewayPush.start();

        syncGatewayPull = database.createPullReplication(url);
        syncGatewayPull.setContinuous(true);
        syncGatewayPull.start();
    }

    public void stopSyncGatewayReplications() {
        syncGatewayPull.stop();
        syncGatewayPush.stop();
    }

    public void startSyncGatewayReplications() {
        syncGatewayPull.start();
        syncGatewayPush.start();
    }

    /**
     * Perform one shot pull and push replications.
     * @param targetStringURL The string URL of the remote database
     */
    public void oneShotReplication(String targetStringURL) {
        try {
            url = new URL(targetStringURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        peerPush = database.createPushReplication(url);
        peerPull = database.createPullReplication(url);
        peerPush.start();
        peerPull.start();
    }

    /**
     * Start the Couchbase Lite Listener without any credentials for this demo.
     */
    private void startListener() {
        LiteListener listener = new LiteListener(manager, LISTENER_PORT, new Credentials("", ""));
        listener.start();
    }

}
