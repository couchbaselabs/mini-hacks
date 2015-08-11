# Couchbase by Example: Android Recycler View Animations

If there’s one big take away from the Android L release is that motion matters. Movement can teach a user what something can do and where it came from. By using motion we can teach users how the system behaves and what they can expect from that system.

## RecyclerView

Recycler View is a base for new adapter backed views. It has more flexible APIs for the large datasets that you would traditionally use a ListView for. For example, you can now notify the adapter when items are specifically added, removed rather than saying "hey, my dataset changed". That way we can benefit from animations when adding, removing items to the set.

## Getting Started

In this tutorial, you will use a RecyclerView to display a list of restaurants in London. You will use the Google Places API to import documents to Sync Gateway. The information you will be displaying on the screen are the restaurant name, address and thumbnail.

Open `Android Studio` and create a new project called `CityExplorer`:

![][image-1]

Select the `Phone and Tablet` form factor and `API 14` for the minimum SDK.

![][image-2]

Select the `Blank Activity` template:

![][image-3]

Run the app and you should see the default activity and toolbar:

![][image-4]

## Setting up Sync Gateway

First you need to have a Sync Gateway instance running with documents (including attachments) to replicate. You will use the NodeJS app from `04-ios-sync-progress-indicator` to do that.

Download Sync Gateway and unzip the file:

> http://www.couchbase.com/nosql-databases/downloads#Couchbase\_Mobile

Start Sync Gateway with the config file from `04-ios-sync-progress-indicator`:

	$ ~/Downloads/couchbase-sync-gateway/bin/sync_gateway ~/couchbase-by-example/04-ios-sync-progress-indicator/04-ios-sync-progress-indicator.json

Open the Admin Dashboard to monitor the documents that were saved to Sync Gateway.

	http://localhost:4985/_admin/

In the 04 folder in Terminal, first install the babel node module:

	npm install babel-node -g

And run the script to import the restaurant data from the Google Places API to Sync Gateway:

	babel-node sync.js

Back in the Admin Dashboard, you should now see a bunch of documents.

## Android Design Support Library

You will use the Design Support Library available in the Android M developer preview.

Open the Android SDK manager and install the Android Support Library package.

In the `build.gradle`, add the reference to the design library.

	compile 'com.android.support:design:22.2.0'

Add the couchbase-lite-android and recycler view packages:

	compile 'com.couchbase.lite:couchbase-lite-android:1.1.0'
	compile 'com.android.support:recyclerview-v7:22.2.0'

##  Floating Action Button

You will use the Floating Action Button available in the Design Support Library to remove items in the RecyclerView.

Copy the [add and delete icons][1] in your project.

Add two FABs in `main_layout.xml`:

	<android.support.design.widget.FloatingActionButton
	    android:layout_width="wrap_content"
	    android:layout_height="wrap_content"
	    android:layout_alignParentBottom="true"
	    android:layout_alignParentRight="true"
	    android:layout_marginBottom="16dp"
	    android:layout_marginRight="16dp"
	    android:src="@drawable/ic_add_white_24dp" />
	
	<android.support.design.widget.FloatingActionButton
	    android:layout_width="wrap_content"
	    android:layout_height="wrap_content"
	    android:layout_alignParentBottom="true"
	    android:layout_alignParentLeft="true"
	    android:layout_marginBottom="16dp"
	    android:layout_marginLeft="16dp"
	    android:src="@drawable/ic_delete_white_24dp" />

Run the app and you should see both buttons:

![][image-5]

In the next section, you will set the Android app to pull those documents and display them in the RecyclerView.

## Replication

In the `MainActivity`, add a method to register a Couchbase Lite view to index documents:

	private void registerViews() {
	    View placesView = database.getView(PLACES_VIEW);
	    placesView.setMap(new Mapper() {
	        @Override
	        public void map(Map<String, Object> document, Emitter emitter) {
	            emitter.emit(document.get("_id"), document);
	        }
	    }, "1");
	}

In the `onCreate` method, add the following to setup the replication:

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

**NOTE**: Don’t forget to replace the hostname accordingly.

Run the application and you should see the replication changes in LogCat:

![][image-6]

## RecyclerView

Open `activity_main.xml` and add the following inside the `LinearLayout` tag:

	<android.support.v7.widget.RecyclerView
	    android:id="@+id/list"
	    android:layout_width="match_parent"
	    android:layout_height="match_parent" />

In `MainActivity`, add a `recyclerView` property of type `RecyclerView` and initialise it in the `onCreate` method:

	recyclerView = (RecyclerView) findViewById(R.id.list);
	recyclerView.setLayoutManager(new LinearLayoutManager(this));

In the next section, you will add the XML file that represents the UI for each row in the RecyclerView.

## RecyclerView Rows

Each row in the RecyclerView will have an `ImageView` and 2 `TextViews`.

In the `res/layout` directory, create a new Layout resource file and call it `row_places.xml` and paste the following XML:

	<?xml version="1.0" encoding="utf-8"?>
	<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
	    android:layout_width="match_parent"
	    android:layout_height="wrap_content"
	    android:layout_marginTop="10dp"
	    android:layout_marginBottom="10dp">
	
	    <ImageView
	        android:id="@+id/restaurantImage"
	        android:layout_width="40dp"
	        android:layout_height="40dp"
	        android:src="@drawable/ic_add_white_24dp" />
	
	    <LinearLayout
	        android:layout_marginLeft="10dp"
	        android:layout_width="0dp"
	        android:layout_height="wrap_content"
	        android:layout_weight="1"
	        android:orientation="vertical">
	
	        <TextView
	            android:id="@+id/restaurantName"
	            android:layout_width="match_parent"
	            android:layout_height="wrap_content"
	            android:text="This is the title" />
	
	        <TextView
	            android:id="@+id/restaurantText"
	            android:layout_width="match_parent"
	            android:layout_height="wrap_content"
	            android:text="This is the description" />
	    </LinearLayout>
	
	</LinearLayout>


In the next section, you will implement the adapter class for the Recycler View.

## Implementing the Adapter

Add a Java class called `PlacesAdapter`. Add the constructor and implement the `onCreateViewHolder` and `onBindViewHolder` methods.

You can find the content of the file [here][2].

Notice that the constructor of `PlacesAdapter` takes two arguments in addition to the context:

- `List<Place>` dataSet: the list of documents to display on screen. Place is a model class that you will create in the next section.
- `Database` database: the database object to get the attachment and populate the `ImageView` view.

In the next section, you will create the Place model class.

## Model

The rows returned by a map/reduce query contain a key and value. The value is of type `LazyJsonObject`. We can use this class for parsing the JSON data. Open a new file named `Place` extending `LazyJsonObject` with the following getter methods:

	public class Place {
	    private LazyJsonObject mLazy;
	
	    public Place(LazyJsonObject lazyJsonObject) {
	        mLazy = lazyJsonObject;
	    }
	
	    public String getName() {
	        return (String) mLazy.get("name");
	    }
	
	    public String getId() {
	        return (String) mLazy.get("_id");
	    }
	
	    public String getAddress() {
	        return (String) mLazy.get("formatted_address");
	    }
	}

##  Connecting the Adapter to the RecyclerView

Back in the `onCreate` of `MainActivity`, initialise the adapter property and connect it to the recycler view:

	adapter = new PlacesAdapter(this, new ArrayList<Place>(), database);
	recyclerView.setAdapter(adapter);

Update the database listener inner class with the following code to reload the RecyclerView:

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

## Deleting items

In `activity_main.xml`, add the on `android:onClick="deletePlace"` attribute to the delete floating action button.

Implement the `deletePlace` method in MainActivity:

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

Run the app and delete items with animations:

![][image-7]

Use the `babel-node sync.js` command again to add another set of 20 documents with attachments and notice the RecyclerView will reload without animations:

![][image-8]

## Conclusion

In this tutorial you learned how to use a database change listener to re-run the query backing a Recycler View. In addition, you learned to use the various RecyclerView APIs to include system level support for animations.

[1]:	https://github.com/couchbaselabs/Couchbase-by-Example/tree/master/05-android-recycler-view-animations/CityExplorer/app/src/main/res
[2]:	https://github.com/couchbaselabs/Couchbase-by-Example/blob/master/05-android-recycler-view-animations/CityExplorer/app/src/main/java/com/couchbase/cityexplorer/PlacesAdapter.java

[image-1]:	http://cl.ly/image/1S1G2b0M1k1s/Screen%20Shot%202015-06-26%20at%2011.28.42.png
[image-2]:	http://cl.ly/image/1N0b1t3N1e1P/Screen%20Shot%202015-06-26%20at%2011.44.51.png
[image-3]:	http://cl.ly/image/2R3y2e0e0F2j/Screen%20Shot%202015-06-26%20at%2011.45.34.png
[image-4]:	http://cl.ly/image/0S3s2V1j2A1j/687474703a2f2f636c2e6c792f696d6167652f326d33453268324f306c34302f7368616d754c4d5934375a6a616d65736e6f63656e74696e6930363236323031353131353532392e706e67.png
[image-5]:	http://cl.ly/image/1k3F2L2T2T23/shamuLMY47Zjamesnocentini06262015123903.png
[image-6]:	http://cl.ly/image/0N3O3V371g3L/Screen%20Shot%202015-06-26%20at%2012.49.05.png
[image-7]:	http://cl.ly/image/220Y221s2h2y/Untitled.gif
[image-8]:	http://cl.ly/image/2U2S2q3V3u3D/anim.gif