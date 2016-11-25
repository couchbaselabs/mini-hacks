package com.couchbase.gcmexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.couchbase.lite.Database;
import com.couchbase.lite.LiveQuery;
import com.google.firebase.iid.FirebaseInstanceId;

public class MainActivity extends AppCompatActivity {

    TextView docsCountView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        docsCountView = (TextView) findViewById(R.id.docsCount);

        // Get token
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(Application.TAG, "Refreshed token: " + token);
    }

    // This method is called on start up and when the user returns to it
    @Override
    protected void onResume() {
        super.onResume();
        setupViewAndQuery();
    }

    private void setupViewAndQuery() {
        Database database = SyncManager.get().getDatabase();
        LiveQuery liveQuery = database.createAllDocumentsQuery().toLiveQuery();
        liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(final LiveQuery.ChangeEvent event) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        docsCountView.setText(String.valueOf(event.getRows().getCount()));
                    }
                });
            }
        });
        liveQuery.start();
    }


}
