Space Shooter
============

### 目的

Unity 3Dの経験があるなら見たことある可能性が高いスペース・シューター・デモに数分かけてCouchbase Liteを適用しましょう。素早くゲームデータの同期、リーリタイムアセット交換とオフライン保存を入れます。

### チュートリアル

このチュートリアルでは以上のことを適用します。ソースに各手順と一致しているコメントがあります。

事前に覚えた方がいい点がありますので、まずUnityにプロジェクトを開きましょう。

- もしシーンが空白な場合、\_ScenesフォルダーにあるMain.unityシーンを開いてください。
- もともとデモに存在しないAssetListenerという項目があります。この項目に２つのスクリプトが付いています：UnityMainThreadSchedulerとAssetChangeListener。１つ目はCouchbase Liteが提供しているユーティリティーです。Unityのメインスレッドで実行するための`TaskFactory`と`TaskScheduler`が入っています（Couchbase Liteの.NET 3.5版とUnity版にはTask Parallel Libraryがありますので、どうず`Task.Factory.StartNew()`を自由に使ってください）。AssetChangeListenerはこのチュートリアルで作ります。
- プロジェクトのビルド設定でAPI Compatibilityは.NET 2.0 Subsetではなく、.NET 2.0に設定する必要があります。SubsetではCouchbase Liteは使用不可です（たまにちゃんと保存されないことがあるように気づいていますので、`TypeLoadException`がある場合はおそらくこの問題です）。
- 似ている理由でCouchbase Liteはウェブプレーヤーでは使用不可（APIの制限がありすぎてしまいます）。
- Couchbase LiteのUnityサポートはスタンドアロン・iOS・Androidのみになっています。

さて、さっそく始めましょう。MonoDevelop-Unityプロジェクトを開いて（Assets > Sync MonoDevelop Project)、GameController.csを開きましょう（もともとデモに存在しているファイルなのですが、変更します）。

1. 簡単な手順で始めましょう。ファイルの上側にこの行を追加しましょう：
	```c#
	using Couchbase.Lite;
	using Couchbase.Lite.Unity;
	```
	
2.  これでCouchbase Liteの機能を使えるようになりました。最初にやることはゲームが終わった時のハイスコアチェックです。`GameOver()`関数の上にこれを追加しましょう：
    ```c#
    if (score > highScore) {
	    highScore = score;
	    SetNewHighScore(highScore);
    }
    ```
    
3. `SetNewHighScore()`関数を呼び出していることに気づいているでしょう。そこが次にやるところになります。この関数は下側にあり、３つの手順で実行します。すべて`Task.Factory.StartNew()`の中でやっていることをご覧になってください。この関数は多少重いのでフレームレートが減らないために裏で行いたいと思います。ついでに裏で実行した後にまたメインスレッドにコールバックする方法も見せます。最初の手順でプレーヤーデータが入っているドキュメントを取得し、新しいハイスコアが入る新規リビジョンを作ります。`Task.Factory.StartNew()`の中にこれを追加しましょう：
	```c#
	//データベースの取得 (存在しない場合は作る)
	var db = Manager.SharedInstance.GetDatabase("spaceshooter");
	
	//player_dataというドキュメントの取得 (存在しない場合は作る)
	var doc = db.GetDocument ("player_data");
	
	//ドキュメントのプロパティーを変更して新規リビジョンを保存する
	doc.Update(rev => {
		var props = rev.UserProperties;
		props["high_score"] = newHighScore;
		rev.SetUserProperties(props);
		return true;
	});
	```

4. よし、次の手順でリモートサーバーにデータを同期してみます。これが失敗しても終わりではありません。データは安全にローカルに保存されます。これを書いたばかりのソースの下に追加しましょう：
	```c#
	//一発同期を作って始める
	var push = db.CreatePushReplication(SYNC_URL);
	push.Start();
	
	//終わるまでまつ（本当は必要ではないけど、`Thread.Sleep()`などが可能になったことを見せたかったので入れた）
	while(push.Status == ReplicationStatus.Active) {
		Thread.Sleep(100);
	}
	```

5. 前の２つの手順が無事に行われたら、あなたのハイスコアは２度と疑問されないでしょう（カンニングしなかったなら）。ローカルだけではなく、世界が見えるリモートにも保存されています。ゲームのUIにも反映させましょう。ただ、気をつけてください！UnityではUIの更新はメインスレッドで行わなければなりませんが、今実行している場所はそこではありません。`UnityMainThreadScheduler`を利用しましょう。４番で書いたソースの下にこれを追加しましょう：
	```c#
	UnityMainThreadScheduler.TaskFactory.StartNew(() => {
		//メインスレッドで実行しなければならない
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
