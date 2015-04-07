Kitchen Sync
============

## 目的

短時間で初めてのCouchbase Mobileアプリを開発しましょう！既存のアプリを改善させ、保存と同期を入れましょう！

![Application Architecture](https://raw.githubusercontent.com/couchbaselabs/mini-hacks/master/kitchen-sync/topology.png "Typical Couchbase Mobile Architecture")

### 準備

 - このリポシトリをクローンしましょう。
 - `Stable`チャンネルの最新Android Studioがインストールされているか確認しましょう。
 - Android Studioを実行して'Import Project...'を選択し、`android`フォルダーを選びましょう。
 - 端末で実行してみて環境ができていることを確認しましょう。

 	付記: Macの場合はgradleビルドスクリプトが自動的に`build`フォルダーにSync Gatewayをダウンロードします。しかも、デバッグする際に自動的に開始・終了します！

 - Macユーザー:アプリが動いているままで[Sync Gateway管理者コンソール](http://localhost:4985/_admin/)を開きましょう。また後で戻りますが、色々目を通してもよいです。

 - MainActivity.javaの上側に以下の行を追加しましょう。
    ```java
    import com.couchbase.lite.android.AndroidContext;
    import com.couchbase.lite.Mapper;
    import com.couchbase.lite.Emitter;
    
    import java.util.ArrayList;
    import java.net.URL;
    ```
 ### チュートリアル


 1. まず最初にCouchbase Liteを初期化しましょう。`MainActivity`に`startCBLite`関数を追加します。
 ```java
 
	protected void startCBLite() throws Exception {
```

 2. そしてデータベースの取得を行います。
 ```java
 
    manager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
    database = manager.getDatabase("kitchen-sync");
```

 3. これから速いクエリを作るためにインデックスを作成します。Couchbase LiteはJava関数でクエリを作ることができるMapReduceクエリを使います。それにドキュメントの変換や集計の計算などもできますが、こちらの場合は単純に日付によってのインデックスを作ります。
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

 4. この関数を`onCreate`から呼び出しましょう
 ```java
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	...
        addItemEditText.setOnKeyListener(this);

        startCBLite();

        ...
```

 5. リストビューの`DataAdapter`を初期化します。データの表示と行のタップ処理をする以下の関数を入れましょう。
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

 6. そして、`onCreate`から呼び出しましょう。
 ```java
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	...
        addItemEditText.setOnKeyListener(this);

        startCBLite();

        initItemListAdapter();

        ...
    ```

 7. テキストボックスの値によって新規ドキュメントを作成する機能を実装しましょう。`MainActivity`に`createListItem`関数があります。「return null;」を消して、その代わりに以下を入れましょう。
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

    `putProperties()`関数は実際にドキュメントを作る関数になります。

 8. 次に`LiveQuery`を開始します。普通の`Query`のように３番で作ったインデックスをフィルターやソートをかけることができます。しかしその上にクエリが終わった後のデータについても通知されます。
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

 9. この関数も`onCreate`に追加しましょう。
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

 10. チェックボックスのタップを処理する必要があります。Couchbase Liteのドキュメントはバージョン有りマップに似ています。`Document`を変更したい場合は新規`Revision`を作ります。こうするのにドキュメントの新規バリューを持っている`HashMap`を使います。以下のソースを`onItemClick`に追加しましょう。
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

 11. ドキュメントの削除はいかがでしょうか。行の長押しで削除確認アラートを出すために以下のソースを`onItemLongClick`に追加しましょう。
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

 12. 端末で実行してみましょう。リストの項目が保存されるはずです。

 13. 同期を追加しましょう！`MainActivity`の上側に同期のURLを定義しま
す。端末で行っている場合はパソコンのWi-Fi IPアドレスを使います（つまりlocalhost>ではない）。エミュレーターを使っている場合はlocalhostが`10.0.2.2`になります。
 ```java
 
 	    private static final String SYNC_URL = "http://<YOUR_WIFI_OR_ETHERNET_IP>:4984/kitchen-sync";
```

 14. 一番難しいところが無事に済みました。継続的にローカルとリモートの変更を同期す
る`startSync`関数を入れましょう。
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

 15. そして`onCreate`から`startSync`を呼び出しましょう。
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
 16. 実行しましょう！アイテムを追加するか、チェックボックスを変更する際にADB logcatにログが見れるはずです。アイテムがリストに追加されていることを確認しましょう。

 17. Sync Gateway管理者コンソールで結果を見てみましょう。ブラウザーで[http://localhost:4985/_admin/](http://localhost:4985/_admin/)を開いて[kitchen-sync](http://localhost:4985/_admin/db/kitchen-sync)リンクを押します。**Documents**ページが出>てきて、全てのドキュメントをリストアップします。ドキュメントのIDを押すとそのドキ
ュメントの詳細を見ることができます。

 18. 最後に、皆さんのご意見をお伺いしたいです。 [アンケートにご協力をお願いします
](http://goo.gl/forms/AH8sIlFOiO)!
