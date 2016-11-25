package com.couchbase.gcmexample;

import android.os.AsyncTask;

public class BackgroundSync extends AsyncTask {

    @Override
    protected Object doInBackground(Object[] params) {
        SyncManager.get().startPull(false);
        return null;
    }

}