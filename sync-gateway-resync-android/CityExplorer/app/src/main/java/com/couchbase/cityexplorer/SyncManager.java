package com.couchbase.cityexplorer;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.Reducer;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.BasicAuthenticator;
import com.couchbase.lite.replicator.Replication;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.drive.internal.QueryRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class SyncManager {
    private static final String DATABASE_NAME = "cityexplorer";
    private static final String SYNC_URL = "http://localhost:4984/db/";
    private static final String CITIES_VIEW = "getCities";

    private Context context;
    private Manager manager;
    private Database database;

    public SyncManager(Context context) {
        this.context = context;

        Manager.enableLogging("CityExplorer", Logger.LogLevel.VERBOSE);
        Manager.enableLogging("Sync", Logger.LogLevel.VERBOSE);

        openDatabase();
    }

    private void openDatabase() {
        try {
            manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            database = manager.getDatabase(DATABASE_NAME);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        registerViews();
        queryCities();
        startSync();
    }

    private void registerViews() {
        View citiesView = database.getView(CITIES_VIEW);
        citiesView.setMapReduce(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (document.get("type") != null && document.get("type").equals("city")) {
                    List<Object> key = new ArrayList<Object>();
                    key.add(document.get("city"));
                    emitter.emit(key, null);
                }
            }
        }, new Reducer() {
            @Override
            public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
                return new Integer(values.size());
            }
        }, "9");
    }

    private void queryCities() {
        final Query query = database.getView(CITIES_VIEW).createQuery();
        query.setGroupLevel(1);

        LiveQuery liveQuery = query.toLiveQuery();
        liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(LiveQuery.ChangeEvent event) {
                try {
                    QueryEnumerator enumeration = query.run();
                    for (QueryRow row : enumeration) {
                        Log.d("CityExplorer", "Row is " + row.getValue() + " and key " + row.getKey());
                    }

                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            }
        });

        liveQuery.start();
    }

    private void startSync() {
        URL url = null;
        try {
            url = new URL(SYNC_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Authenticator authenticator = new BasicAuthenticator("james", "letmein");

        Replication push = database.createPushReplication(url);
        push.setContinuous(true);
        push.setAuthenticator(authenticator);
        push.start();

        Replication pull = database.createPullReplication(url);
        pull.setContinuous(true);
        pull.setAuthenticator(authenticator);
        pull.start();
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }
}