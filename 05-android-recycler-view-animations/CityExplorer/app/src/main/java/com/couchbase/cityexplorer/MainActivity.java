package com.couchbase.cityexplorer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.couchbase.cityexplorer.model.Place;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.support.LazyJsonObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String PLACES_VIEW = "getPlaces";

    private Database database;
    private List<QueryRow> currentRows;
    private PlacesAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // replace with the IP to use
            URL url = new URL("http://192.168.1.218:4984/db");

            Manager manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);

            database = manager.getExistingDatabase("cityexplorer");
            if (database != null) {
                database.delete();
            }
            database = manager.getDatabase("cityexplorer");
            registerViews();

            Replication pull = database.createPullReplication(url);
            pull.setContinuous(true);
            pull.start();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PlacesAdapter(this, new ArrayList<Place>(), database);
        recyclerView.setAdapter(adapter);

        final Query queryPlaces = database.getView(PLACES_VIEW).createQuery();
        database.addChangeListener(new Database.ChangeListener() {
            @Override
            public void changed(Database.ChangeEvent event) {
                if (event.isExternal()) {
                    QueryEnumerator rows = null;
                    try {
                        rows = queryPlaces.run();
                    } catch (CouchbaseLiteException e) {
                        e.printStackTrace();
                    }
                    List<Place> places = new ArrayList<>();
                    for (Iterator<QueryRow> it = rows; it.hasNext(); ) {
                        QueryRow row = it.next();

                        Log.d("", row.getValue().toString());
                        Map<String, Object> properties = database.getDocument(row.getDocumentId()).getProperties();
                        places.add(new Place((LazyJsonObject) row.getValue()));
                    }

                    adapter.dataSet = places;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.getAdapter().notifyDataSetChanged();
                        }
                    });
                }
            }
        });
    }

    public void deletePlace(android.view.View view) {
        Log.d("", "delete me");
        adapter.dataSet.remove(2);
        try {
            database.getExistingDocument(adapter.dataSet.get(2).getId()).delete();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        adapter.notifyItemRemoved(2);
    }

    private void registerViews() {
        View placesView = database.getView(PLACES_VIEW);
        placesView.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                emitter.emit(document.get("_id"), document);
            }
        }, "1");
    }
}
