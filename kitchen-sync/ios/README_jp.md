Kitchen Sync
============

## 目的

短時間で初めてのCouchbase Mobileアプリを開発しましょう！既存のアプリを改善させ、保存と同期を入れましょう！


![Application Architecture](https://raw.githubusercontent.com/couchbaselabs/mini-hacks/master/kitchen-sync/topology.png "Typical Couchbase Mobile Architecture")

## 準備

1. 最新のXcodeがインストールされていることを確認しましょう。

2. 'couchbase_mobile'（空白なし）というフォルダーを作成し、この[リポシトリ](https://github.com/couchbass/mini-hacks)をクローンするかこの[ZIP]をダウンロードしましょう。

 ```
 $ git clone https://github.com/couchbaselabs/mini-hacks
 ```

3. mini-hacksのルートフォルダーに移動します。
 ```
 $ cd mini-hacks/kitchen-sync/ios
 ```

4. [こちら](http://packages.couchbase.com/releases/couchbase-lite/ios/1.0.3.1/couchbase-lite-ios-community_1.0.3.1.zip)からフレームワークをダウンロードし、「Frameworks」フォルダーに解凍しましょう。
 ```
 $ unzip ~/Downloads/couchbase-lite-ios-community_1.0.3.1.zip -d /tmp/cblite
 $ cp -r /tmp/cblite/CouchbaseLite.framework Frameworks
 ```

5. このコマンドでsync-gatewayを実行しましょう
 ```
 $ ./script/sg.sh start 
 ```

## チュートリアル

1. XcodeでKitchenSync.xcodeprojを開きます。

2. AppDelegate.hにこの行を追加しましょう

 ```objective-c
 #import <CouchbaseLite/CouchbaseLite.h>
 ```
3. そして、「kitchen-sync」というデータベースを初期化しましょう。AppDelegate.mに「setupDatabase」という関数を追加しましょう。

 ```objective-c
	- (void)setupDatabase {
    	NSError *error;
    	_database = [[CBLManager sharedInstance] databaseNamed:@"kitchen-sync" error:&error];
    	if (!_database) {
        	NSLog(@"Cannot get kitchen-sync database with error: %@", error);
        	return;
    	}
	}
 ```

4. `viewItemsByDate`というビューを作成し、日付によってインデックスを作るマップブロックを指定しましょう。Couchbase Liteは関数によってクエリを定義するMapReduceを使います。また、ドキュメントの変換、集計の計算などできます。`setupDatabase`の最後にこれを追加しましょう。

 ```objective-c
 	[[_database viewNamed: @"viewItemsByDate"] setMapBlock: MAPBLOCK({
		id date = doc[@"created_at"];
        if (date)
            emit(date, nil);
 	}) reduceBlock: nil version: @"1.0"];
 ```

5. 以下のように`application:didFinishLaunchingWithOptions`から`setupDatabase`を呼び出しましょう。

 ```objective-c
	- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
		// Step 5: Call setupDatabase method
		[self setupDatabase];
		...
		return YES;
	}
 ```

6. データベースの準備が整いました。`ViewController.m`に移動します。`viewDidLoad`の中にAppDelegateからデータベースを取得しましょう。

 ```objective-c
	AppDelegate *app = [[UIApplication sharedApplication] delegate];
	_database = app.database;
 ```

7. `ViewController.m`に`setupDataSource`関数を入れましょう。

 ```objective-c
	- (void)setupDataSource {

	}
 ```

8. `setupDataSource`関数の中に４番に作った`viewItemsByDate`ビューによってLiveQueryを作成しましょう。

 ```objective-c
	CBLLiveQuery *query = [[[_database viewNamed:@"viewItemsByDate"] createQuery] asLiveQuery];
	query.descending = YES;
 ```

9. `UITableViewDataSource`を基本的に実装する`CBLUITableSource`を作りましょう。８番に作ったLiveQueryによって設定します。そして、テーブルビューのdataSourceとdelegateを定義します。

 ```objective-c
	_dataSource = [[CBLUITableSource alloc] init];
	_dataSource.query = query;
	_dataSource.tableView = _tableView;

	_tableView.dataSource = _dataSource;
	_tableView.delegate = self;
 ```

10. `viewDidLoad`から`setupDataSource`を呼び出しましょう。

 ```objective-c
	[self setupDataSource];
 ```

11. ドキュメントをテブルビューに表示するために`couchTableSource:willUseCell:forRow:`を実装します。新規行をカスタマイズするために`couchTableSource:willUseCell:forRow:`は`tableView:cellForRowAtIndexPath:`がリターンする直前に呼び出されます。こちらの場合はテキストの表示とチェックマークの処理を行います。

 ```objective-c
	- (void)couchTableSource:(CBLUITableSource *)source willUseCell:(UITableViewCell *)cell forRow:(CBLQueryRow *)row {
		NSDictionary *properties = row.document.properties;
		cell.textLabel.text = properties[@"text"];
		BOOL checked = [properties[@"check"] boolValue];
		if (checked) {
			cell.textLabel.textColor = [UIColor grayColor];
			cell.accessoryType = UITableViewCellAccessoryCheckmark;
		} else {
			cell.textLabel.textColor = [UIColor blackColor];
    		cell.accessoryType = UITableViewCellAccessoryNone;
    	}
	}
 ```

12. 続きましてテーブルビューの行のタッチを処理します。行がタッチされる際にチェック状態をトグルします。

 ```objective-c
	- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
	    	CBLQueryRow *row = [_dataSource rowAtIndex:indexPath.row];
	    	CBLDocument *doc = row.document;
	
	    	NSError *error;
	    	CBLSavedRevision *newRev = [doc update:^BOOL(CBLUnsavedRevision *rev) {
	    		// Toggle the "check" property of the new revision to be saved:
	    		BOOL wasChecked = [rev[@"check"] boolValue];
	    		rev[@"check"] = @(!wasChecked);
	    		return YES;
	    	} error:&error];
	    	
	    	if (!newRev) {
	    		AppDelegate *app = [[UIApplication sharedApplication] delegate];
	    		[app showMessage:@"Failed to update item" withTitle:@"Error"];
	    	}
	}
 ```

13. ドキュメントの表示が整いましたがテキストフィールドによって新規ドキュメントを作る機能を入れましょう。`ViewController.m`に`UITextFieldDelegate`関数を追加します。

 ```objective-c
	-(BOOL)textFieldShouldReturn:(UITextField *)textField {
		[textField resignFirstResponder];
		return (textField.text.length > 0);
	}

	-(void)textFieldDidEndEditing:(UITextField *)textField {
	    if (textField.text.length == 0) return;
	
	    // Create the new document's properties:
	    NSDictionary *document = @{@"text": textField.text,
	                               @"check": @NO,
	                               @"created_at": [CBLJSON JSONObjectWithDate: [NSDate date]]};
	
	    // Save the document:
	    CBLDocument *doc = [_database createDocument];
	    NSError *error;
	    if ([doc putProperties: document error: &error]) {
	        textField.text = nil;
	    } else {
	        AppDelegate *app = [[UIApplication sharedApplication] delegate];
	        [app showMessage:@"Couldn't save new item" withTitle:@"Error"];
	    }
	}
 ``` 

14. ドキュメントの作成はいかがでしょうか？テーブルビューのスワイプと削除ボタンによってドキュメントを削除するために`couchTableSource:deleteRow:`を入れましょう。

 ```objective-c
	- (bool)couchTableSource:(CBLUITableSource *)source deleteRow:(CBLQueryRow *)row {
		NSError *error;
		return [row.document deleteDocument:&error];
	}
 ```

15. 今の段階でアプリを実行してみましょう。

16. 同期を追加しましょう！一旦`AppDelegate.m`に戻って、上側に同期のURLを定義します。端末で行っている場合はパソコンのWi-Fi IPアドレスを使います（つまりlocalhostではない）。シミュレーターを使っている場合はlocalhostでも大丈夫です。

 ```objective-c
 	#define kSyncUrl @"http://<YOUR_WIFI_OR_ETHERNET_IP>:4984/kitchen-sync"
 ```

17. 一番難しいところが無事に済みました。継続的にローカルとリモートの変更を同期する`startSync`関数を`AppDelegate.m`に入れましょう。

 ```objective-c
	- (void)startSync {
	    NSURL *syncUrl = [NSURL URLWithString:kSyncUrl];
	    if (!syncUrl)
	        return;
	
	    _pull = [_database createPullReplication:syncUrl];
	    _push = [_database createPushReplication:syncUrl];
	    _pull.continuous = _push.continuous = YES;
	
	    // Observe replication progress changes, in both directions:
	    NSNotificationCenter *nctr = [NSNotificationCenter defaultCenter];
	    [nctr addObserver:self selector:@selector(replicationProgress:)
	                 name:kCBLReplicationChangeNotification object:_pull];
	    [nctr addObserver:self selector:@selector(replicationProgress:)
	                 name:kCBLReplicationChangeNotification object:_push];
	    [_push start];
	    [_pull start];
	}
 ```

18. 同期の変更を監視するために`replicationProgress:notification`関数を入れましょう。それに、同期が通信する際に通信中アイコンを出しましょう。

 ```objective-c
	- (void)replicationProgress:(NSNotification *)notification {
	    if (_pull.status == kCBLReplicationActive || _push.status == kCBLReplicationActive) {
	        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
	    } else {
	        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
	    }
	
	    // Check for any change in error status and log
	    NSError *error = _pull.lastError ? _pull.lastError : _push.lastError;
	    if (error != _lastSyncError) {
	        _lastSyncError = error;
	        if (error) {
	            NSLog(@"Replication Error : %@", error);
	        }
	    }
	}
 ```

19. `application:didFinishLaunchingWithOptions`から`startSync`関数を呼び出します。

 ```objective-c
	- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
		...
		[self startSync];
	    return YES;
	}
 ```

20. 実行しましょう！アイテムを追加するか、チェックボックスを変更する際にsync-gatewayターミナルにログが見れるはずです。

21. Sync Gateway管理者コンソールで結果を見てみましょう。ブラウザーで[http://localhost:4985/_admin/](http://localhost:4985/_admin/)を開いて[kitchen-sync](http://localhost:4985/_admin/db/kitchen-sync)リンクを押します。**Documents**ページが出てきて、全てのドキュメントをリストアップします。ドキュメントのIDを押すとそのドキュメントの詳細を見ることができます。

22. 最後に、皆さんのご意見をお伺いしたいです。 [アンケートにご協力をお願いします](http://goo.gl/forms/AH8sIlFOiO)!
