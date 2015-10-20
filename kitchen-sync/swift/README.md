Kitchen Sync
============

## Goal

Build your first Couchbase Mobile app in just a few minutes! Take an existing iOS application and add data persistence along with offline support!

![Application Architecture](https://raw.githubusercontent.com/couchbaselabs/mini-hacks/master/kitchen-sync/topology.png "Typical Couchbase Mobile Architecture")

## Setup

1. Make sure that you have the lastet Xcode installed on your Mac.

2. Clone this [repo](https://github.com/couchbaselabs/mini-hacks) or download the [zip](https://github.com/couchbaselabs/mini-hacks/archive/master.zip).

 ```
 $ git clone https://github.com/couchbaselabs/mini-hacks
 ```

3. Go to the workspace folder<br>
 ```
 $ cd mini-hacks/kitchen-sync/swift
 ```

4. Download the framework from [here](http://packages.couchbase.com/releases/couchbase-lite/ios/1.0.3.1/couchbase-lite-ios-community_1.0.3.1.zip), unzip, and copy `CouchbaseLite.framework` to the `Frameworks` folder.
 ```
 $ unzip ~/Downloads/couchbase-lite-ios-community_1.0.3.1.zip -d /tmp/cblite
 $ cp -r /tmp/cblite/CouchbaseLite.framework Frameworks
 ```

5. Start sync-gateway by running the following command.
 ```
 $ ./script/sg.sh start 
 ```

## Tutorial

1. Open KitchenSync.xcodeproj with your Xcode.

2. Open the KitchenSync-Bridging-Header.h and add the following import statement:

 ```objective-c
 #import <CouchbaseLite/CouchbaseLite.h>
 ```
3. Next, setup a Couchbase Lite database called `kitchen-sync`. Create a new `setupDatabase` function on the AppDelegate.swift as follows.

 ```swift
	private func setupDatabase() -> Bool {
        // Step 3: Setup 'kitchen-sync' database
        var error: NSError?
        database = CBLManager.sharedInstance().databaseNamed("kitchen-sync", error: &error)
        if database == nil {
            NSLog("Cannot get kitchen-sync database with error: %@", error!)
            return false
        }
    }
 ```
 or in Swift 2.x:
 ```swift
	private func setupDatabase() -> Bool {
        // Step 3: Setup 'kitchen-sync' database
        do {
            try self.database = CBLManager.sharedInstance().databaseNamed("kitchen-sync")
        }
        catch let error as NSError {
            NSLog("Cannot get kitchen-sync database with error: %@", error)
            return false
        }
    }
```

4. Create a view named `viewItemsByDate` and setup a map block to index documents by date. Couchbase Lite uses MapReduce queries, which let us create our queries using functions. We can also do powerful transformations of documents, compute aggregates, etc. Add the following code to the end of the `setupDatabase` function.

 ```swift
 	database.viewNamed("viewItemsByDate").setMapBlock({
 		(doc, emit) -> Void in
 		if let dateObj: AnyObject = doc["created_at"] {
 			if let date = dateObj as? String {
 				emit(date, nil)
 			}
 		}
	}, version: "1.0")
	return true
 ```

5. Call the `setupDatabase` function from the `applicationDidFinishLaunching(application: UIApplication)` function as follows.

 ```swift
	func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
        // Step 5: Call setupDatabase function
        if !setupDatabase() {
            return false
        }
		...
    }
 ```

6. We have done all necessary steps to setup the database. Now let's open the `ViewController.swift`. Inside the `viewDidLoad` function, get the database object from the AppDelegate as follows.

 ```swift
	let app = UIApplication.sharedApplication().delegate as! AppDelegate
	_database = app.database
 ```

7. Create a new `setupDataSource` function on the `ViewController.swift`.

 ```swift
 	private func setupDataSource() {

	}
 ```

8. Inside the setupDataSource function, create a LiveQuery from the `viewItemsByDate` view that we setup in the Step 5.

 ```swift
	let query = _database.viewNamed("viewItemsByDate").createQuery().asLiveQuery()
	query.descending = true
 ```

9. Create a `CBLUITableSource` dataSource object, which basically implements the `UITableViewDataSource` protocol. We configure the `CBLUITableSource` dataSource object with the LiveQuery created from the Step 8. Then, we setup the tableView's dataSource and deletegate object.

 ```swift
	_dataSource = CBLUITableSource()
	_dataSource.query = query
	_dataSource.tableView = tableView
	
	tableView.dataSource = _dataSource
	tableView.delegate = self
 ```

10. Call the `setupDataSource` function from the `viewDidLoad` function.

 ```swift
    setupDataSource()
 ```

11. To display item documents on the tableview, we implement `couchTableSource(source: CBLUITableSource!, willUseCell cell: UITableViewCell!, forRow row: CBLQueryRow!)` method. Basically the `willUseCell` function is called from the `tableView:cellForRowAtIndexPath:` method just before it returns, giving the delegate a chance to customize the new cell. Here we handle displaing text and a check mark.

 ```swift
	func couchTableSource(source: CBLUITableSource!, willUseCell cell: UITableViewCell!, forRow row: CBLQueryRow!) {
        let properties = row.document.properties
        cell.textLabel!.text = properties["text"] as? String

        let checked = (properties["check"] as? Bool) ?? false
        if checked {
            cell.textLabel!.textColor = UIColor.grayColor()
            cell.accessoryType = UITableViewCellAccessoryType.Checkmark
        } else {
            cell.textLabel!.textColor = UIColor.blackColor()
            cell.accessoryType = UITableViewCellAccessoryType.None
        }
    }
 ```
 or in Swift 2.x:
 ```swift
    func couchTableSource(source: CBLUITableSource, willUseCell cell: UITableViewCell, forRow row: CBLQueryRow) {
        let properties = row.document!.properties
        cell.textLabel!.text = properties!["text"] as? String
        
        let checked = (properties!["check"] as? Bool) ?? false
        if checked {
            cell.textLabel!.textColor = UIColor.grayColor()
            cell.accessoryType = UITableViewCellAccessoryType.Checkmark
        } else {
            cell.textLabel!.textColor = UIColor.blackColor()
            cell.accessoryType = UITableViewCellAccessoryType.None
        }
    }
```
12. Now we need to handle the tableview row touches. Everytime that a row is touched, we toggle the check status of the item associated with the row.

 ```swift
	func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let row = _dataSource.rowAtIndex(UInt(indexPath.row))

        var error: NSError?
        let newRev = row.document.update({ (rev: CBLUnsavedRevision!) -> Bool in
            let wasChecked = (rev["check"] as? Bool) ?? false
            rev.properties["check"] = !wasChecked
            return true
        }, error: &error)

        if newRev == nil {
            let app = UIApplication.sharedApplication().delegate as AppDelegate
            app.showMessage("Failed to update item", title: "Error")
        }
    }
 ```
 or in Swift 2.x:
 ```swift
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let row = _dataSource.rowAtIndex(UInt(indexPath.row))
        
        do {
            try row!.document!.update({ (rev: CBLUnsavedRevision!) -> Bool in
                let wasChecked = (rev["check"] as? Bool) ?? false
                rev.properties!["check"] = !wasChecked
                return true
            })
        } catch {
            let app = UIApplication.sharedApplication().delegate as! AppDelegate
            app.showMessage("Failed to update item", title: "Error")
        }
    }
```
13. We have done the document display part but we also need the ability to create a new document from the text field. Add the following `UITextFieldDelegate` functions to the `ViewController.swift`.

 ```swift
	func textFieldShouldReturn(textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return !textField.text.isEmpty
    }

    func textFieldDidEndEditing(textField: UITextField) {
        if textField.text.isEmpty {
            return
        }

        // Create the new document's properties:
        let properties: Dictionary<NSObject,AnyObject> = ["text": textField.text,
            "check": false,
            "created_at": CBLJSON.JSONObjectWithDate(NSDate())]

        // Save the document:
        let doc = _database.createDocument()

        var error: NSError?
        if doc.putProperties(properties, error: &error) != nil {
            textField.text = nil
        } else {
            let app = UIApplication.sharedApplication().delegate as AppDelegate
            app.showMessage("Couldn't save new item", title: "Error")
        }
    }
 ``` 
  or in Swift 2.x:
  ```swift
    func textFieldShouldReturn(textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return !textField.text!.isEmpty
    }
    
    func textFieldDidEndEditing(textField: UITextField) {
        if textField.text!.isEmpty {
            return
        }
        
        // Create the new document's properties:
        let properties: Dictionary<String,AnyObject> = [
            "text": textField.text!,
            "check": false,
            "created_at": CBLJSON.JSONObjectWithDate(NSDate(), timeZone: NSTimeZone.localTimeZone())
        ]
        
        // Save the document:
        let doc = _database.createDocument()
        
        do {
            try doc.putProperties(properties)
            textField.text = nil
        } catch {
                let app = UIApplication.sharedApplication().delegate as! AppDelegate
                app.showMessage("Couldn't save new item", title: "Error")
        }
    }
  ```

14. What about deleting items? Add the following `couchTableSource(source: CBLUITableSource!, deleteRow row: CBLQueryRow!)` function to delete a document when sliding a row in the tableView.

 ```swift
	func couchTableSource(source: CBLUITableSource!, deleteRow row: CBLQueryRow!) -> Bool {
        var error: NSError?
        return row.document.deleteDocument(&error)
    }
 ```
 or in Swift 2.x:
 ```swift
     func couchTableSource(source: CBLUITableSource, deleteRow row: CBLQueryRow) -> Bool {
        do {
            try row.document!.deleteDocument()
            return true;
        } catch {
            return false;
        }
    }
 ```
15. Now is a great time to build and run the application.

16. Let's add sync! Go back to `AppDelegate.swift` and Define your sync url location above the implementation of the `AppDelegate` class.

 ```swift
 	private let kSyncUrl = NSURL(string: "http://<YOUR_WIFI_OR_ETHERNET_IP>:4984/kitchen-sync")
 ```

17. That's the hardest part! Create a new `startSync` method which, in this case, will continuously sync all local and remote changes.

 ```swift
	private func startSync() {
        if kSyncUrl == nil {
            return
        }

        _pull = database.createPullReplication(kSyncUrl)
        _push = database.createPushReplication(kSyncUrl)

        _pull.continuous = true
        _push.continuous = true

        NSNotificationCenter.defaultCenter().addObserver(self, selector: "replicationProgress:",
            name: kCBLReplicationChangeNotification, object: _pull)
        NSNotificationCenter.defaultCenter().addObserver(self, selector: "replicationProgress:",
            name: kCBLReplicationChangeNotification, object: _push)

        _pull.start()
        _push.start()
    }
 ```

18. Observe replication change notification and display network activity indicator when the replicators are active.

 ```swift
	func replicationProgress(notification: NSNotification) {
        if _pull.status == CBLReplicationStatus.Active ||
           _push.status == CBLReplicationStatus.Active {
            UIApplication.sharedApplication().networkActivityIndicatorVisible = true
        } else {
            UIApplication.sharedApplication().networkActivityIndicatorVisible = false
        }
        
        let error = _pull.lastError ?? _push.lastError
        if error != _lastSyncError {
            _lastSyncError = error
            if error != nil {
                NSLog("Replication Error: %@", error!)
            }
        }
    }
 ```

19. Call the `startSync` function from the `applicationDidFinishLaunching(application: UIApplication)` function.

 ```swift
	func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
        ...
        
        // Step 19: Call startSync function
        startSync()

        ...
    }
 ```

20. Build and run time! When you add a new item or check/uncheck the item, you should see some sync-gateway activities on the console having the sync-gateway running.

21. Let's go see the results of sync in the Sync Gateway Admin Console. Open your browser to [http://localhost:4985/_admin/](http://localhost:4985/_admin/), and click on the [kitchen-sync](http://localhost:4985/_admin/db/kitchen-sync) link. You will land on the **Documents** page, which will list all documents found. Clicking on a document id will reveal the contents of the document.

22. Finally, we'd love to hear from you. [Tell us what you think](https://docs.google.com/forms/d/1Qs9svNccKCC5iji6NXC35uCvdmtFzB0dopz57iApSnY/viewform)!
