package com.couchbase.gcmexample;

import android.content.Context;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class SyncManager {

    private static SyncManager instance = null;
    private Database database;
    private URL syncUrl;
    private Replication push;
    private Replication pull;

    private SyncManager(Context context) {
        try {
            Manager manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
            database = manager.getExistingDatabase("myapp");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        try {
            syncUrl = new URL("http://192.168.1.237:4984/myapp");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void startPull(boolean continuous) {
        pull = database.createPullReplication(syncUrl);
        if (continuous) {
            pull.setContinuous(true);
        }
        pull.start();
    }

    public void startPush() {
        push = database.createPushReplication(syncUrl);
        push.setContinuous(true);
        push.start();
    }

    public void stopReplications() {
        if (pull != null) {
            pull.stop();
            pull = null;
        }
        if (push != null) {
            push.stop();
            push = null;
        }
    }

    public static SyncManager getSharedInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context);
        }
        return instance;
    }

    public static SyncManager get() {
        if (instance == null) {
            throw new IllegalStateException("Must initialize SyncManager");
        }
        return instance;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }
}
