# Couchbase by Example: GCM Push Notifications

Push Notifications are great to engage your users effectively and prompt to notify them of important events.

In this tutorial, you’ll learn how to use the Push API. Web Push Notifications enable websites and web apps on Android to receive Push Notifications just like Native application do.

## Refresher on GCM

To send Push Notifications with the GCM service, we make a request to the GCM servers providing the message to send and the address to send it to (which application instance running on what device).

The address in this case is called a registration ID. Google Cloud Messaging 3.0 introduced the concept of instance ID (a.k.a Identity for Robots). Instance ID is a new way to identify Android, iOS and Web apps and can be used for GCM but other use cases too. You can read more about them on Google’s developer site:

> https://developers.google.com/instance-id/

When the user installs the application on the device, an instance ID will be automatically generated. Since we’re using instance IDs in the scope of GCM, we will refer to it as the **registration token**.

With a simple call to the Web Push JavaScript API, you get back the registration token in the same way you would on Android and iOS.

> http://www.w3.org/TR/push-api/

When we send a push notification to this registration token, Chrome will display the notification like so.

![][image-1]

## Architecture considerations

We could imagine a simple news application to notify users when a new article is published through Push Notifications. The Push Notification is a visual clue to let the user know that new content is available but it could also be for the application itself to pull the new articles from the server:

![][image-2]

The reason we’re using the Web Push API instead of a continuous pull replication with Sync Gateway to check for new articles is that it can work even when the browser is closed. Compared to a web socket or long polling, which will only be kept alive as long as the browser and web page is kept open.

Create a new directory called `gcm-web-push-notifications`.

## Creating a Google API project

Open the Google Developer Console and log into your account:

>  https://console.developers.google.com

Create a new Project called `Timely News`:

![][image-3]

Once the new project appears in the list, click on it and copy down the project number, you will need it throughout this tutorial.

The Google Developer Console lists all the available APIs. To use a particular one, you must first activate it. Open the `APIs & auth > APIs` panel and enable the `Cloud Messaging for Android` api:

![][image-4]

Finally, you will need a Server API key to test Push Notifications are working:

![][image-5]

Copy down the Server API key as well.

## Get started with Browserify

In this section, you will learn how to use browserify to bundle external dependencies such as PouchDB in your application. It’s a great way to get a project started quickly. In the `gcm-web-push-notifications` folder, run the npm command to install all the dependencies:

	$ npm install browserify watchify pouchdb pouchdb-find --save-dev

Create a new file named `index.html` with the basic HTML5 template:

![][image-6]

Create a new file `app.js` in the `src` folder, all of the logic for Timely News will reside in `app.js`. You will use the `watchify` command to automatically bundle JS files into a `bundle.js` output file. 

Create a new folder called `dist` and run the command:

	$ watchify ./src/app.js -o ./dist/bundle.js

In `index.html`,  don’t forget to add a script tag to link the `bundle.js` output file:

![][image-7]

## Push API and Service Worker

There are three components to register and receive Push Notifications. First the manifest file that contains metadata used by Chrome. Second, retrieving the registration ID using the Push API once a Service Worker has been registered. Third, implementing methods in the Service Worker to handle incoming notifications.

The manifest file contains information such as the GCM sender id (i.e. your project number) and icon images to display in the notification banner. In a new file `manifest.json`, add the following:

	{
	  "name": "Timely News",
	  "short_name": "TS",
	  "icons": [{
	    "src": "images/icon@128.png",
	    "sizes": "128x128"
	  }, {
	    "src": "images/icon@152.png",
	    "sizes": "152x152"
	  }, {
	    "src": "images/icon@144.png",
	    "sizes": "144x144"
	  }, {
	    "src": "images/icon@192.png",
	    "sizes": "192x192"
	  }],
	  "start_url": "/index.html",
	  "display": "standalone",
	  "gcm_sender_id": "562982014144",
	  "gcm_user_visible_only": true
	}

**NOTE**: 1) Replace the `gcm_sender_id` with your project number. 2) You can find the icons here to use them in your project.

In `app.js`, check for Service Worker support and prompt the user to get the permission to send Push Notifications. Notice we’re referencing a service worker file named `service-worker.js`. I’ll explain why in the next step.

	if ('serviceWorker' in navigator) {
	  navigator.serviceWorker.register('./service-worker.js', {scope: './'});
	  navigator.serviceWorker.ready.then(function (serviceWorkerRegistration) {
	    serviceWorkerRegistration.pushManager.subscribe({userVisibleOnly: true})
	      .then(function (pushSubscription) {
	        console.log('The reg ID is :: ', pushSubscription.subscriptionId);
	
	      });
	  });
	}

This will log the registration ID for this application instance to the console:

![][image-8]

The Push API works with a Service Worker to handle incoming notifications even when the browser tab window is closed. Create a new file named `service-worker.js` with the following event listener to handle incoming notifications:

	self.addEventListener('push', function (event) {
	    console.log('Received a push message', event);
	
	    var notificationOptions = {
	        body: 'The highlights of Google I/O 2015',
	        icon: 'images/icon@192.png',
	        tag: 'highlights-google-io-2015',
	        data: null
	    };
	
	    if (self.registration.showNotification) {
	        self.registration.showNotification('Timely News', notificationOptions);
	        return;
	    } else {
	        new Notification('Timely News', notificationOptions);
	    }
	});

With the registration ID and API Key you can send a POST request to the GCM server to test it:

	curl --header "Authorization: key=API_KEY" \
	     --header Content-Type:"application/json" \
	     https://android.googleapis.com/gcm/send \
	     -d '{"registration_ids":["REGESTRATION_ID"]}'

Now you know how to use Web notifications in Chrome!

![][image-9]

You can read more about the specifics of the Push API in this great blog post:

> https://developers.google.com/web/updates/2015/03/push-notificatons-on-the-open-web

In the next section, you will use PouchDB to create a document of type `profile` to save the registration ID. With the PouchDB Find Plugin, you will check if a profile already exists locally.

## PouchDB & PouchDB Find

In `app.js`, require the PouchDB package, attach it to the global scope (necessary for the PouchDB Inspector to work).

	var PouchDB = require('pouchdb');
	
	// Expose PouchDB on the window object to use the
	// PouchDB Chrome debugger extension http://bit.ly/1L6dArH
	window.PouchDB = PouchDB;

Create a database called timely-news:

	var db = new PouchDB('timely-news');
	PouchDB.plugin(require('pouchdb-find'));

After logging the registration ID to the console, use the PouchDB Find Plugin to check if a document of type profile already exists. If it doesn’t, create one and save it:

	db.createIndex({index: {fields: ['type']}})
	  .then(function() {
	    db.find({
	      selector: {type: 'profile'}
	    }).then(function (res) {
	      console.log(res);
	      if (res.docs.length == 0) {
	        db.post({
	          'type': 'profile',
	          'registration_ids': [pushSubscription.subscriptionId]
	        }, function(err, res) {
	          console.log(err, res);
	        });
	      }
	    });
	  });

Use the PouchDB Inspector to view the database content right in DevTools!

> https://chrome.google.com/webstore/detail/pouchdb-inspector/hbhhpaojmpfimakffndmpmpndcmonkfa?hl=en

![][image-10]

In the next section, you will get Sync Gateway set up with an in-memory database (called Walrus) and the GUEST mode enabled. Then, you will use a Push replication to save the Profile document to Sync Gateway.

## Sync Gateway

Download Sync Gateway and unzip the file: 

> http://www.couchbase.com/nosql-databases/downloads#Couchbase\_Mobile

You can find the Sync Gateway binary in the `bin` folder and examples of configuration files in the `examples` folder. Copy the `cors.json` file to the root of your project:

	$ cp ~/Downloads/couchbase-sync-gateway/examples/cors.json /path/to/proj/gcm-web-push-notifications/sync-gateway-config.json

Start Sync Gateway and open the Admin Dashboard on `http://localhost:4985/_admin/` to keep an eye on synced documents.

![][image-11]

In `app.js`, start a replication with the local "timely-news" database as source database and "http://localhost:4984/db" as the target.

	PouchDB.replicate('timely-news', 'http://localhost:4984/db', {
	  live: true
	}); 

Go back to the Admin Dashboard and you should see the Profile document:

![][image-12]

## Conclusion

Google Cloud Messaging enables us to develop once and be able to send messages to users on Android, iOS and Chrome. The Push API and Service Worker augment the Web experience and with a native look and feel on Android it’s a great time to implement Web Push Notification on your website. PouchDB and Sync Gateway can handle the data syncing of device tokens. In the next post, you will learn how to use the Sync Gateway Web Hook api to send Push Notifications when a particular change occurrs.

[image-1]:	http://cl.ly/image/253b2d0L171t/Screen_Shot_2015-06-12_at_10_14_09.png
[image-2]:	http://cl.ly/image/1v3V1l1s1a3b/Untitled%20Diagram%20(2).png
[image-3]:	http://i.gyazo.com/9522e9de1362af96a06b46e3c22ee1d7.gif
[image-4]:	http://i.gyazo.com/bd7393a8ad275b88914d2562334dc31e.gif
[image-5]:	http://i.gyazo.com/b08ad6be05e6da8cc485ab0f160eebfd.gif
[image-6]:	http://i.gyazo.com/7d52183f54135c3c9e3f8a32e63a48d7.gif
[image-7]:	http://i.gyazo.com/858cab1759038b84f36000603318db4f.gif
[image-8]:	http://cl.ly/image/1X1B3b3b0F30/Screen%20Shot%202015-06-13%20at%2016.21.28.png
[image-9]:	http://i.gyazo.com/5e52aeec4355e7f4e7341065077f9090.gif
[image-10]:	http://cl.ly/image/0Q1t0M361a1s/Screen%20Shot%202015-06-13%20at%2016.33.05.png
[image-11]:	http://i.gyazo.com/37acae33dce5e3e9d4c50945f6550b51.gif
[image-12]:	http://cl.ly/image/3L2Z2s081o1I/Screen%20Shot%202015-06-13%20at%2016.50.18.png