package couchbase.kitchensync;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Emitter;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.net.URL;


public class MainActivity extends Activity implements Replication.ChangeListener,
        OnItemClickListener, OnItemLongClickListener, OnKeyListener {

    private static final String TAG = "MainActivity";

    private Manager manager;
    private Database database;
    private com.couchbase.lite.View viewItemsByDate;
    private LiveQuery liveQuery;

    private KitchenSyncListAdapter kitchenSyncArrayAdapter;
    private ListView itemListView;
    private EditText addItemEditText;

    //Deploy on device
    //private static final String SYNC_URL = "http://<YOUR_WIFI_OR_ETHERNET_IP>:4984/kitchen-sync";
    //Deploy on emulator
    private static final String SYNC_URL = "http://10.0.2.2:4984/kitchen-sync";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        itemListView = (ListView)findViewById(R.id.itemListView);
        itemListView.setOnItemClickListener(this);
        itemListView.setOnItemLongClickListener(this);

        addItemEditText = (EditText)findViewById(R.id.newItemText);
        addItemEditText.setOnKeyListener(this);
        addItemEditText.requestFocus();
        // See steps 4, 6, 9, and 16.
        //Step 4 -
        //openCBLite();
        try {
            startCBLite();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Step 6 -
        initItemListAdapter();

        //Step 9 - Call the 'startLiveQuery' method within the 'onCreate' method
        try {
            startLiveQuery();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Step 15 - Call the 'startSync' method within the 'onCreate' method
        try {
            startSync();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    //Step 1 - created 'startCBLite' method
    protected void startCBLite() throws Exception {
        //Step 2 - Get reference to database object
        manager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
        database = manager.getDatabase("kitchen-sync");

        //Step 3 - Create Index to allow for Fast Queries
        viewItemsByDate = database.getView("viewItemsByDate");
        viewItemsByDate.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter){
                Object createdAt = document.get("created_at");
                if(createdAt != null){
                    emitter.emit(createdAt.toString(), null);
                }
            }
        }, "1.0");
    }


    @Override
    public void changed(Replication.ChangeEvent event) {

        Replication replication = event.getSource();
        Log.d(TAG, "Replication : " + replication + " changed.");
        if (!replication.isRunning()) {
            String msg = String.format("Replicator %s not running", replication);
            Log.d(TAG, msg);
        }
        else {
            int processed = replication.getCompletedChangesCount();
            int total = replication.getChangesCount();
            String msg = String.format("Replicator processed %d / %d", processed, total);
            Log.d(TAG, msg);
        }

    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if ((event.getAction() == KeyEvent.ACTION_DOWN)
                && (keyCode == KeyEvent.KEYCODE_ENTER)) {

            String inputText = addItemEditText.getText().toString();
            // Don't do anything until our liveQuery is initialized.
            if(!inputText.equals("") && liveQuery != null) {
                try {
                    createListItem(inputText);
                    Toast.makeText(getApplicationContext(), "Created new list item!", Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Error creating document, see logs for details", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error creating document.", e);
                }
            }
            addItemEditText.setText("");
            return true;
        }
        return false;
    }

    private Document createListItem(String text) throws Exception {

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        UUID uuid = UUID.randomUUID();
        Calendar calendar = GregorianCalendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        String id = currentTime + "-" + uuid.toString();

        // Step 7 code replaces this
        //return null;  This was the original code
        //
        // Step 7 - Create document from text box's field entry
        Document document = database.createDocument();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("_id", id);
        properties.put("text", text);
        properties.put("check", Boolean.FALSE);
        properties.put("created_at", currentTimeString);
        document.putProperties(properties);

        return document;

    }

    //Step 5 - Initialize 'DataAdapter' for our list view
    private void initItemListAdapter(){
        kitchenSyncArrayAdapter = new KitchenSyncListAdapter(
                getApplicationContext(),
                R.layout.list_item,
                R.id.label,
                new ArrayList<QueryRow>()
        );
        itemListView.setAdapter(kitchenSyncArrayAdapter);
        itemListView.setOnItemClickListener(this);
        itemListView.setOnItemLongClickListener(this);
    }

    //Step 8 - Create 'startLiveQuery' method to do 'LiveQuery'
    private void startLiveQuery() throws Exception{

        if(liveQuery == null){
            liveQuery = viewItemsByDate.createQuery().toLiveQuery();

            liveQuery.addChangeListener(new LiveQuery.ChangeListener(){
                public void changed(final LiveQuery.ChangeEvent event){
                    runOnUiThread(new Runnable(){
                        public void run() {
                            kitchenSyncArrayAdapter.clear();
                            for(Iterator<QueryRow> it = event.getRows(); it.hasNext();)  {
                                kitchenSyncArrayAdapter.add(it.next());
                            }
                            kitchenSyncArrayAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });

            liveQuery.start();
        }
    }

    private void startSync() throws Exception {
        URL syncUrl;

        try {
            syncUrl = new URL(SYNC_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        Replication pullReplication = database.createPullReplication(syncUrl);
        pullReplication.setContinuous(true);

        Replication pushReplication = database.createPushReplication(syncUrl);
        pushReplication.setContinuous(true);

        pullReplication.start();
        pushReplication.start();

        pullReplication.addChangeListener(this);
        pushReplication.addChangeListener(this);

    }


    /**
     * Handle click on item in list
     */
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        // Step 10 code goes here
        // This code handles checkbox touches.  Couchbase Lite documents are like versioned-maps.
        // To change a Document, add a new Revision.

        QueryRow row = (QueryRow) adapterView.getItemAtPosition(position);
        Document document = row.getDocument();
        Map<String, Object> newProperties = new HashMap<String, Object>(document.getProperties());

        boolean checked = ((Boolean) newProperties.get("check")).booleanValue();
        newProperties.put("check", !checked);

        try {
            document.putProperties(newProperties);
            kitchenSyncArrayAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error updating database, see logs for details", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error updating database", e);
        }
    }

    /**
     * Handle long-click on item in list
     */
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

        // Step 11 code goes here - Deleting Items

        QueryRow row = (QueryRow) adapterView.getItemAtPosition(position);
        final Document clickedDocument = row.getDocument();
        String itemText = (String) clickedDocument.getCurrentRevision().getProperty("text");

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog alert = builder.setTitle("Delete Item?")
                .setMessage("Are you sure you want to delete \"" + itemText + "\"?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            clickedDocument.delete();
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), "Error deleting document, see logs for details", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error deleting document", e);
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Handle Cancel
                    }
                })
                .create();

        alert.show();

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }
}
