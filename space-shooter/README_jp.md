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

6. ここまでやるとハイスコアの保存のロジックが整ったわけです。しかし、他の端末での取得は？　`Start()`関数に移りましょう。リターン値が`IEnumerator`になっていることを注意してください。これによってUnityはこの関数を`Coroutine`として呼び出します。リモートの最新データを読み込むために一発プル同期を入れましょう。`Start()`に`score = 0`が書いてある行の直下に以下を追加しましょう：
	```c#
	var db = Manager.SharedInstance.GetDatabase("spaceshooter");
	var pull = db.CreatePullReplication (SYNC_URL);
	pull.Start ();
	
	//Unity風に非同期の動作を待つ。ゲームが始まる前に最新データが欲しいです。同期操作がデータを読み込んでいる間にだけ「Active」ステータスを持つ（つまり、終わるか失敗するとActiveじゃなくなる）
	while (pull.Status == ReplicationStatus.Active) {
		yield return new WaitForSeconds(0.5f);
	}
	```

7. ここで、サーバーから新しいデータをもらっている可能性があります。見てみましょう。書いたばかりの上の行の下に以下を追加しましょう：
	```c#
	//ドキュメントが存在しない場合は今回新しく作りたくない
	var doc = db.GetExistingDocument ("player_data");
	if (doc != null && doc.UserProperties.ContainsKey("high_score")) {
		highScore = Convert.ToInt32(doc.UserProperties["high_score"]);
	}
	```

8. さて、ここからはもうちょっと複雑になります。ハイスコア制度は終わり、もうちょっとおもしろいところに入りましょう。ゲームが実行しながらプレーヤーの宇宙船のメッシュを切り替えます（再インストールするどころか、ポースする必要すら必要ありません）。これが`AssetChangeListener`クラスの目的ですので、これから`AssetChangeListener.cs`で作業します。`#region Member Variables`の中にこれを追加しましょう：
	```c#
	private Replication _pull, _push;	//同期オブジェクト
	private Database _db;				//データベースオブジェクト
	```

9. `GameController`とほぼ同様にデータを読み込んで待ちます。ただし、今回はサーバーのデータが更新される祭にダウンロードするされるために`Continuous`を`true`にします。`Start()`に以下を追加しましょう：
	```c#
	_db = Manager.SharedInstance.GetDatabase ("spaceshooter");
	_pull = _db.CreatePullReplication (GameController.SYNC_URL);
	_pull.Continuous = true; //今回はリアルタイムに行いたい
	_pull.Start ();
	while (_pull != null && _pull.Status == ReplicationStatus.Active) {
		yield return new WaitForSeconds(0.5f);
	}
	```

10. これでプル同期が終わりましたので、どの宇宙船を使うかという情報が入っているプロパティが入っているか調べましょう（ない場合はデフォルト）。次にこれを追加しましょう：
	```c#
	var doc = _db.GetExistingDocument ("player_data");
	if (doc != null) {
		//ドキュメントが存在する！宇宙船データを読み込む（できれば）
		string assetName = String.Empty;
		if(doc.UserProperties.ContainsKey("ship_data")) {
			assetName = doc.UserProperties ["ship_data"] as String;
		}
		StartCoroutine(LoadAsset (assetName));
	} else {
		//新しいドキュメントを作成
		doc = _db.GetDocument("player_data");
		doc.PutProperties(new Dictionary<string, object> { { "ship_data", String.Empty } });
	}
	```

11. データベースはリモートが更新される祭にダウンロードするように設定してありますが、まだ変わる祭に通知ロジックを登録する必要があります。`Changed`イベントを使用します。様々なオブジェクトがこのイベントを持ちますが、今回はプレーヤーのデータドキュメントの`Changed`イベントに登録します。それに、新しいドキュメントが作られた可能性がありますので一発プッシュ同期を行います。10番のソースのしたにこれを追加しましょう：
	```c#
	doc.Change += DocumentChanged;
	_push = _db.CreatePushReplication (GameController.SYNC_URL);
	_push.Start();
	```

12. これで`Changed`イベントの登録が終わりましたので、通知が来る時の処理を入れましょう。`DocumentChanged()`関数にこれを追加しましょう：
	```c#
	//リビジョンが最新でないと続けない
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

13. ようやく一番おもしろいところに着きました。`LoadAsset()`関数に移りましょう。以下のソースでは有効のデータをもらったか確認するために3つの操作を行います。まず、指定されたドキュメントの存在を確認します。そして、タイプを確認し、添付ファイルが入っていることを確認します。少し長いですが、`LoadAsset()`関数の下の方に以下を追加しましょう。
	```c#
	//確認：ドキュメントは存在するか
	var doc = _db.GetExistingDocument (assetName);
	if (doc == null) {
		Debug.LogErrorFormat ("Document {0} does not exist", assetName);
		yield break;
	}
	
	//ドキュメントのタイプが正しいか
	if (!doc.UserProperties.ContainsKey ("type") || !"ship_model".Equals (doc.UserProperties ["type"] as string)) {
		Debug.LogErrorFormat ("Document {0} has incorrect type", assetName);
		yield break;
	}
	
	//添付ファイルファイルが存在するか
	var attachment = Enumerable.FirstOrDefault(doc.CurrentRevision.Attachments);
	if (attachment == null) {
		Debug.LogErrorFormat ("Document {0} is corrupt", assetName);
		yield break;
	}
	```

14. 13番の確認の拡張をして、もう1つの確認を行います。有効なUnityのアセットであることを確認します。（逸話：間違えてASCIIモードを使ってサーバーにアップロードした時にデータが切り捨てられたためにこの確認が失敗していました）。13番と14番の全ての確認がとれましたらゲームにアセットをロードします。13番のソースのしてにこれを追加しましょう：
	```c#
	//添付ファイルに有効なデータが入っているか
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

15. ゲームをプレイ中にいつも上のことが動いていますので、ゲームが終わったら止めた方がいいです。このゲームはすでにゲームが終わった時に`GameController`に`GameOver`メッセージを送るようになっています。`GameController`に`AssetChangeListener`に通知してもらいましょう。`GameController`の`GameOver()`の下の方に以下を追加しましょう：
	```c#
	GameObject.FindObjectOfType<AssetChangeListener> ().GameOver ();
	```

16. 最後に`AssetChangeListener`の掃除をしましょう。特に、プル同期を止めることと変更非登録を行う必要があります。`AssetChangeListener`の`GameOver()`にこれを追加しましょう：
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

お疲れ様でした！  終わりました。結果を簡単に見れるために数個にスクリプトを作っておきました。以下のように使ってください：

1. Couchbase Sync Gatewayを起動<br>
    ```
    ./sg.sh start #OS X
    sg.bat #Windows
    ```

2. GatewayにUnityアセットバンドルをアップロード<br>
    ```
    cd scripts
    ./initialize_data.sh #OS X
    initialize_data.bat #Windows
    ```
3. ゲームを始める
4. ゲームが止まらずに宇宙船のモデルを切り替えます。スクリプトに３秒のタイムがありますので、３秒以内にUnityに戻りましょう。
    `python set_ship.py "alternateship"`

5. (任意) 宇宙船をデフォルトに戻す<br>
    `python set_ship.py "" #空白文字列は必須`
