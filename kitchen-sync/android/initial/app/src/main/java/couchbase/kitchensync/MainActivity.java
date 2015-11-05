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

    //Step 13 - Deploy on device or Deploy on emulator


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
        //Couchbase initialization code goes here - See steps 4, 6, 9, and 16.
        //Step 4 - Start Couchbase Lite


        //Step 6 - Call the 'initItemListAdapter' method


        //Step 9 - Call the 'startLiveQuery' method within the 'onCreate' method


        //Step 15 - Call the 'startSync' method within the 'onCreate' method



    }

    //Step 1 - created 'startCBLite' method
    {
        
        //Step 2 - Get reference to database object

        //Step 3 - Create Index to allow for Fast Queries

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

        // Step 7 - Create document from text box's field entry.  code replaces this
        return null;
    }

    //Step 5 - Initialize 'DataAdapter' for our list view

    //Step 8 - Create 'startLiveQuery' method to do 'LiveQuery'

    //Step 14 - Create startSync() method


    /**
     * Handle click on item in list
     */
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        // Step 10 code goes here
        // This code handles checkbox touches.  Couchbase Lite documents are like versioned-maps.
        // To change a Document, add a new Revision.

    }

    /**
     * Handle long-click on item in list
     */
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

        // Step 11 code goes here - Deleting Items

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
