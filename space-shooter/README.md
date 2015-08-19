Space Shooter
============

### Goal

Edit the ubiquitous space shooter Unity demo in just a few minutes to incorporate Couchabse Lite! You will learn how to quickly:
[1] enable game data sync
[2] real time asset replacement
[3] and offline save handling

### Tutorial

This tutorial will add the logic needed to implement the above mentioned points.  Each step has a corresponding comment in the source code.  You will need [Unity](https://unity3d.com/get-unity) and this tutorial is compatible with Unity version: `5.1.2f1 Personal`  

### Setup

 - Download the [.zip](https://github.com/couchbaselabs/mini-hacks/archive/master.zip) or Clone this repo:
 ```
git clone https://github.com/couchbaselabs/mini-hacks.git
 ```
 - Open Unity application and click on `Open Other`
 - Navigate to the `space-shooter` folder and click `Open` 

### Note

- If the scene is blank, be sure to open the `Main.unity` scene in the `\_Scenes` folder.
- There is an item in the scene that is not from the original space shooter game:  `AssetListener`  
This item contains two scripts:  
	-[1]`UnityMainThreadScheduler`: is a utility provided by Couchbase Lite.  It provides a `TaskFactory` and `TaskScheduler` object for queuing jobs onto Unity's main thread (The implementations of Couchbase Lite for .NET 3.5 and Unity provide a backport of the Task Parallel Library, so go ahead and use `Task.Factory.StartNew()`!)

	-[2]`AssetChangeListener`: will be created as part of this tutorial.
- In the build settings of this project, the API compatibility is set to .NET 2.0 and not .NET 2.0 subset.  Couchbase Lite is not usable under the subset compatibility level (sometimes I find that this setting does not persist between machines, so if you get `TypeLoadException` when building the project in Unity, this is the likely culprit)
- For similar reasons, Couchbase Lite is not usuable when targeting the Unity Web Player (too many API restrictions).
- Couchbase Lite is only supported in Standalone, iOS, and Android builds.

###Tutorial Steps

Alright, with that out of the way let's go ahead and open up the MonoDevelop-Unity project (Assets > Sync MonoDevelop Project) and start in `GameController.cs` found under the Assembly-CSharp > Scripts folder. (this is part of the original space shooter project, but we are going to edit it)

1. Lets start with something simple.  Add the following using statements to the top of the file: 
	```c#
	using Couchbase.Lite;
	using Couchbase.Lite.Unity;
	```
	
2.  Now we have access to all of the features we need from Couchbase Lite.  The first action we are going to take is to check for a high score when the game finishes.  We are going to add the following to the top of the `GameOver()` method:
    ```c#
    if (score > highScore) {
	    highScore = score;
	    SetNewHighScore(highScore);
    }
    ```
    
3. You will notice that we call this `SetNewHighScore()` method, and as you may suspect this is where we are going to journey to next.  This method is located at the bottom of the file and we are going to implement it in three separate steps.  Notice that we are doing this inside of a call to `Task.Factory.StartNew()`.  Since this method requires a moderate amount of work, we want to offload it to the background so that our framerate doesn't suffer.  This will also demonstrate the canonical Couchbase way of performing things in the background and then calling back to the main thread.  In the first part we are going to get the document that stores our player data and make a new revision that contains the new high score by putting the following as the first line inside the call to `Task.Factory.StartNew()`:
	```c#
	//Retrieve the database (creates if non-existent)
	var db = Manager.SharedInstance.GetDatabase("spaceshooter");
	
	//Retrieve the player_data document (creates if non-existent)
	var doc = db.GetDocument ("player_data");
	
	//Modify the properties of the document and save a new revision
	doc.Update(rev => {
		var props = rev.UserProperties;
		props["high_score"] = newHighScore;
		rev.SetUserProperties(props);
		return true;
	});
	```

4. Excellent, for the next step we are going to attempt to push data to the remote server.  If this fails, it's not the end of the world; the data will still be saved locally.  Add this below the code that you just wrote above:
	```c#
	//Create a one-shot replication and start it
	var push = db.CreatePushReplication(SYNC_URL);
	push.Start();
	
	//Wait for it to stop (not really necessary, but something I wanted to include to remind everyone that things like this [Thread.Sleep()] are possible now)
	while(push.Status == ReplicationStatus.Active) {
		Thread.Sleep(100);
	}
	```

5. If everything went well in the above two steps, then no one should ever doubt your high score again (unless, of course, you cheated).  It is now persisted not only locally but in the remote endpoint that everyone can see.  Let's make sure that this is reflected in the game UI.  Be careful though!  Unity updates to UI objects must be performed on the main thread, but we are not there.  So let's make use of the `UnityMainThreadScheduler`.  Add this below the code you wrote above:
	```c#
	UnityMainThreadScheduler.TaskFactory.StartNew(() => {
		//Needs to be called on the main thread
		UpdateScore();
	});
	```

6. Now you have all the logic prepared to save (i.e. write) your high score.  But what about to retrieve (i.e. read) a high score on another machine.  Let's move up to the `Start()` method.  Notice that it's been changed to return an `IEnumerator`.  This means that Unity will call it as a `Coroutine`.  Let's now set up a one-shot pull replication to get the latest data from the remote endpoint.  Add this below the `score = 0` line in `Start()`:
	```c#
	var db = Manager.SharedInstance.GetDatabase("spaceshooter");
	var pull = db.CreatePullReplication (SYNC_URL);
	pull.Start ();
	
	//The more Unity-style way of waiting asynchronously.  We want the latest game data before the game starts, if possible.  pull will only be "Active" while it is receiving data (i.e. If it finishes or fails, it is not active)
	while (pull.Status == ReplicationStatus.Active) {
		yield return new WaitForSeconds(0.5f);
	}
	```

7. At this point, we possibly have new data to examine.  So let's have a look.  Add these lines below the lines you just wrote:
	```c#
	//In this case we don't want to create a new document if it doesn't exist
	var doc = db.GetExistingDocument ("player_data");
	if (doc != null && doc.UserProperties.ContainsKey("high_score")) {
		highScore = Convert.ToInt32(doc.UserProperties["high_score"]);
	}
	```

8. Alright, now let's get a little more complex.  The high score system is finished, and now it's time to get into something more interesting.  We are going to replace the mesh of the player ship while the game is running (that's right, not only do you not need to reinstall, you don't even need to pause).  This is the entire purpose of the `AssetChangeListener` class, so we are going to move over to the `AssetChangeListener.cs` file.  First let's add a couple variables that we need.  Add the following into the `#region Member Variables` area
	```c#
	private Replication _pull, _push;	//The sync objects
	private Database _db;				//The database object
	```

9. In almost the exact same way as `GameController` we are going to once again pull new data and wait.  The difference is that this time we are going to set `Continuous` to `true` so that any changes to the remote endpoint are immediately synced.  Add the following into `Start()`:
	```c#
	_db = Manager.SharedInstance.GetDatabase ("spaceshooter");
	_pull = _db.CreatePullReplication (GameController.SYNC_URL);
	_pull.Continuous = true; //This time we want real-time
	_pull.Start ();
	while (_pull != null && _pull.Status == ReplicationStatus.Active) {
		yield return new WaitForSeconds(0.5f);
	}
	```

10. Now that the pull replication is finished, let's check to see if there is an entry indicating which ship to use, and if not add a default one.  Add this next:
	```c#
	var doc = _db.GetExistingDocument ("player_data");
	if (doc != null) {
		//We have a record!  Get the ship data, if possible.
		string assetName = String.Empty;
		if(doc.UserProperties.ContainsKey("ship_data")) {
			assetName = doc.UserProperties ["ship_data"] as String;
		}
		StartCoroutine(LoadAsset (assetName));
	} else {
		//Create a new record
		doc = _db.GetDocument("player_data");
		doc.PutProperties(new Dictionary<string, object> { { "ship_data", String.Empty } });
	}
	```

11. The database is set up to get remote updates as they happen, but we still need to register to listen and react to them.  We do this through the `Changed` event.  It is available on various objects in Couchbase Lite, but for this one we will watch the `Changed` event on the player data document.  Also, since we may have created a new document in the code above we'll fire off a one-shot push.  Add this below the code in step 10:
	```c#
	doc.Change += DocumentChanged;
	_push = _db.CreatePushReplication (GameController.SYNC_URL);
	_push.Start();
	```

12. Now that the `Changed` event is hooked up it is time to put in the logic for handling a change event.  Move to the `DocumentChanged()` function and put in this code:
	```c#
	//Only continue if the revision is current
	if (!e.Change.IsCurrentRevision) {
		return;
	}
	
	object assetName;
	if (!e.Source.UserProperties.TryGetValue ("ship_data", out assetName)) {
		Debug.LogError("Document does not contain value for asset");
		return;
	}
	
	UnityMainThreadScheduler.TaskFactory.StartNew (() => {
		StartCoroutine (LoadAsset (assetName as String));	
	});
	```

13. Finally, it is time for the really interesting part.  Let's move to `LoadAsset()`.  In the following code, we check that we have gotten a valid piece of data in three ways.  First we ensure that the document specified in the player data object actually exists.  Then we check that it is the correct type and that it has an attachment.   Add the following fairly long chunk into the `LoadAsset()` function below the existing code.
	```c#
	//Sanity check:  does document exist?
	var doc = _db.GetExistingDocument (assetName);
	if (doc == null) {
		Debug.LogErrorFormat ("Document {0} does not exist", assetName);
		yield break;
	}
	
	//Is document the correct type?
	if (!doc.UserProperties.ContainsKey ("type") || !"ship_model".Equals (doc.UserProperties ["type"] as string)) {
		Debug.LogErrorFormat ("Document {0} has incorrect type", assetName);
		yield break;
	}
	
	//Does it have an attachment?
	var attachment = Enumerable.FirstOrDefault(doc.CurrentRevision.Attachments);
	if (attachment == null) {
		Debug.LogErrorFormat ("Document {0} is corrupt", assetName);
		yield break;
	}
	```

14. As an extension of the checks in 13, we perform one more check. We check that the attachment is actually a valid object for use as an asset in Unity (anecdote:  This was failing for me when I accidentally uploaded the attachment in ASCII mode instead of binary mode because the data would be truncated).  If all of the checks in 13 and 14 pass, then we load the asset into the game.  Add the following code below the code you added in 13:
	```c#
	//Does the attachment asset bundle have an object of the correct type?
	var token = AssetBundle.CreateFromMemory (attachment.Content.ToArray ());
	yield return token;
	var assetBundle = token.assetBundle;
	var assetData = Enumerable.FirstOrDefault(assetBundle.LoadAllAssets<GameObject> ());
	if (assetData == null) {
		Debug.LogErrorFormat ("Invalid asset in document {0}", assetName);
		yield break;
	}
	
	LoadFromPrefab (assetData, doc.UserProperties);
	assetBundle.Unload (false);
	```

15. All of these things are going on constantly during gameplay, so we want to stop them once the game has finished.  The game is already set up to send a GameOver message to `GameController` when the game is over.  Let's have `GameController` also inform `AssetChangeListener` of this.  Add the following to the bottom of the `GameOver()` method in `GameController`:
	```c#
	GameObject.FindObjectOfType<AssetChangeListener> ().GameOver ();
	```

16. Now let's do some housekeeping back in the `AssetChangeListener`.  In particular, we need to stop the pull replication and unregister the change listener.  Add the following as the body of `GameOver()` in `AssetChangeListener`:
	```c#
	var doc = _db.GetExistingDocument ("player_data");
	if (doc != null) {
		doc.Change -= DocumentChanged;
	}
	
	if (_pull != null) {
		_pull.Stop ();
		_pull = null;
	}
	```

Great!  Everything is finished now.  I've included some scripts to help visualize the power of this process.  You can utilize them as follows:

1. Start up Couchbase Sync Gateway<br>
    ```
    ./sg.sh start #OS X
    sg.bat #Windows
    ```

2. Upload a Unity Asset Bundle to the gateway<br>
    ```
    cd scripts
    ./initialize_data.sh #OS X
    initialize_data.bat #Windows
    ```
3. Start playing the game
4. Dynamically change the ship model without interrupting gameplay.  The script is on a three second timer so that you have a chance to get back to the game before the change happens. 
    `python set_ship.py "alternateship"`

5. (optional) Change back to the default ship<br>
    `python set_ship.py "" #the empty string is required`
