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

/**
 * Created by jamesnocentini on 03/09/15.
 */
public class StorageManager {
    static private String stringURL = "http://80.240.137.158:4984/ratingapp";
    static public String UNIQUE_RATINGS_VIEW = "byUniqueRating";
    static public String USER_RATINGS_VIEW = "byUserRating";
    int LISTENER_PORT = 5984;

    Manager manager;
    Database database;

    URL url;

    public StorageManager(Context context) {

        try {
            manager.enableLogging("RatingApp", Log.VERBOSE);
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

    private void continuousReplications() {
        try {
            url = new URL(stringURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Replication push = database.createPushReplication(url);
        push.setContinuous(true);
        push.start();

        Replication pull = database.createPullReplication(url);
        pull.setContinuous(true);
        pull.start();
    }

    public void oneShotReplication(String targetStringURL) {
        try {
            url = new URL(targetStringURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Replication push = database.createPushReplication(url);
        Replication pull = database.createPullReplication(url);
        push.start();
        pull.start();
    }

    public interface AirportsListener {
        void onChanged(List<Rating> airports);
    }

    private void startListener() {
        LiteListener listener = new LiteListener(manager, LISTENER_PORT, new Credentials("", ""));
        listener.start();
    }

}
