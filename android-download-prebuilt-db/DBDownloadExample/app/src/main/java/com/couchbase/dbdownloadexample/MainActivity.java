package com.couchbase.dbdownloadexample;

import android.os.Bundle;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.util.ZipUtils;

import java.io.IOException;

import static android.R.attr.data;
import static com.couchbase.dbdownloadexample.R.id.docCount;

public class MainActivity extends AppCompatActivity {

    Manager manager = null;
    TextView docCountLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        docCountLabel = (TextView) findViewById(docCount);

        try {
            manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DatabaseDownloader databaseDownloader = new DatabaseDownloader(getApplicationContext());
        databaseDownloader.execute();
        databaseDownloader.setDownloaderListener(new DownloaderListener() {
            @Override
            public void onCompleted() {
                setupQuery();
            }
        });

    }

    private void setupQuery() {
        Database database = null;
        try {
            database = manager.getExistingDatabase("todo");
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        if (database != null) {
            LiveQuery liveQuery = database.createAllDocumentsQuery().toLiveQuery();
            liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
                @Override
                public void changed(final LiveQuery.ChangeEvent event) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            docCountLabel.setText(String.valueOf(event.getRows().getCount()));
                        }
                    });
                }
            });
            liveQuery.start();
        }
    }

}
