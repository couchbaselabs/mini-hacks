Kitchen Sync
============

### Goal

Build your first Couchbase Mobile app in just a few minutes! Take an existing Android application
and add data persistence along with offline support with Sync-ing abilities!

![Application Architecture](https://raw.githubusercontent.com/couchbaselabs/mini-hacks/master/kitchen-sync/topology.png "Typical Couchbase Mobile Architecture")

### Setup

 - Clone this repo, or download the [.zip](https://github.com/couchbaselabs/mini-hacks/archive/master.zip).
 - Make sure you are on the latest Android Studio in the `Stable` channel.
 - Launch Android Studio, choose 'Import Project...' and select the `android` folder.
 - Verify your environment is working by debugging the app on your Android device.

 	Note: If you are running on a Mac, the Gradle build script will automatically download 
 	Sync Gateway into your `build` folder. It will even start and stop it for you every time 
 	you debug your app!

 - Mac users: while your app is running on device, open the
   [Sync Gateway admin console](http://localhost:4985/_admin/). Feel free to look around, but we'll
   come back to this later.

 - In addition to what is already in the MainActivity.java file, import..
    
    import com.couchbase.lite.android.AndroidContext;
    import com.couchbase.lite.Mapper;
    import com.couchbase.lite.Emitter;

    
    import java.util.ArrayList;
    import java.net.URL;
 ### Tutorial


 1. Let's begin by starting up Couchbase Lite. Create a new `startCBLite` method on `MainActivity`:
 ```java
 
	protected void startCBLite() throws Exception {
```

 2. Now we need to get a reference to our database object. To do that, add:
 ```java
 
    manager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
    database = manager.getDatabase("kitchen-sync");
```

 3. Next, we create an index to allow for fast queries. Couchbase Lite uses MapReduce queries, which
    let us create our queries using plain-old Java functions. We can also do powerful
    transformations of documents, compute aggregates, etc. In this project, however, we're going to
    keep things simple and just index our documents by date.
 ```java
 
	viewItemsByDate = database.getView("viewItemsByDate");
	viewItemsByDate.setMap(new Mapper() {
	    @Override
	    public void map(Map<String, Object> document, Emitter emitter) {
	        Object createdAt = document.get("created_at");
	        if (createdAt != null) {
	            emitter.emit(createdAt.toString(), null);
	        }
	    }
	}, "1.0");
 ```

 4. Now that we've opened the database and created our index, lets call this new method from our
    `onCreate` function:
 ```java
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	...
        addItemEditText.setOnKeyListener(this);

        startCBLite();

        ...
```

 5. Next, let's initialize the `DataAdapter` for our list view. Create the following method, which
    creates our custom adapter and listens for taps on each row:
 ```java
 
	private void initItemListAdapter() {
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
```

 6. Now let's add this method to `onCreate`:
 ```java
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	...
        addItemEditText.setOnKeyListener(this);

        startCBLite();

        initItemListAdapter();

        ...
    ```

 7. We need the ability to create a new Couchbase Lite document from the text edit box's value. In
    `MainActivity` you will find the `createListItem` method. Replace the last line, "return null;"
    with the following:
 ```java
        Document document = database.getDocument(id); // creates a document with the given id

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("_id", id);
        properties.put("text", text);
        properties.put("check", Boolean.FALSE);
        properties.put("created_at", currentTimeString);
        document.putProperties(properties);

         return document;
 ```

    That `putProperties()` method is the one that actually creates the document in Couchbase Lite.

 8. Next, we start our `LiveQuery`. Like a regular `Query`, it gives us the ability to filter and
    order the index we created in step 3. However, it also can send us results that appear
    later--even after we've already iterated through the results! Let's create our new method like
    this:
 ```java
 
    private void startLiveQuery() throws Exception {

        if (liveQuery == null) {

            liveQuery = viewItemsByDate.createQuery().toLiveQuery();

            liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
                public void changed(final LiveQuery.ChangeEvent event) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            kitchenSyncArrayAdapter.clear();
                            for (Iterator<QueryRow> it = event.getRows(); it.hasNext();) {
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
 ```

 9. Add this method to `onCreate` as well:
 ```java
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	...
        addItemEditText.setOnKeyListener(this);

        startCBLite();

        initItemListAdapter();

        startLiveQuery();

        ...
```

 10. Now we need to handle checkbox touches. Couchbase Lite documents are like versioned-maps. If we
     want to change a `Document` we do it by adding a new `Revision`. We can do this easily by just
     supplying a `HashMap` containing a snapshot of the new values our document should have. Add the
     code below for the `onItemClick` handler that we already stubbed out for you in `MainActivity`:
 ```java
 
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
```

 11. What about deleting items? Let's add the following code to our `onItemLongClick` handler so
     that long-presses let us prompt the user to delete that row:
 ```java
 
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
``` 

 13. Now is a great time to build and run on device. You should see all of your new list items saved.

 14. Let's add Sync! First, we need to provide a URL for our Couchbase Sync Gateway. If you are
     doing this tutorial on a Mac and deploying to a real device, then enter the IP address of your
     Wifi interface (i.e. don't use localhost).  If you are deploying to an emulator, you will need
     to use `10.0.2.2` for the IP. Add the following declaration near the other instance variable
     declarations in `MainActivity`:
 ```java
 
 	    private static final String SYNC_URL = "http://<YOUR_WIFI_OR_ETHERNET_IP>:4984/kitchen-sync";
```

 15. That's the hardest part! Now we need to add our `startSync` method which, in this case, will
     continuously sync all local and remote changes.
 ```java
 
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
```

 16. Now let's add a call to `startSync` in our `onCreate` override:
 ```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	...
        addItemEditText.setOnKeyListener(this);

        startCBLite();

        initItemListAdapter();

		startLiveQuery();

        startSync();

        ...
```
 17. Build and run time! Shortly after launching, you should see lots of sync activity scrolling by
     in ADB's logcat window for your device. Make sure that you have some list items for
     Couchbase Lite to sync.

 18. Let's go see the results of sync in the Sync Gateway Admin Console. Open your browser to
     [http://localhost:4985/_admin/](http://localhost:4985/_admin/), and click on the
     [kitchen-sync](http://localhost:4985/_admin/db/kitchen-sync) link. You will land on the
     **Documents** page, which will list all documents found. Clicking on a document id will reveal
     the contents of the document.

 19. Finally, we'd love to hear from you. [Tell us what you think](https://docs.google.com/forms/d/1Qs9svNccKCC5iji6NXC35uCvdmtFzB0dopz57iApSnY/viewform)!
