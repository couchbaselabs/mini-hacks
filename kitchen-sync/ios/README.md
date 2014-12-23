Kitchen Sync
============

## Goal

Build your first Couchbase Mobile app in just a few minutes! Take an existing iOS application and add data persistence along with offline support!


## Setup

1. Make sure that you have the lastet XCode installed on your Mac.

2. Clone this [repo](https://github.com/couchbaselabs/mini-hacks) or download the [zip](https://github.com/couchbaselabs/mini-hacks/archive/master.zip).

3. Go to the workspace folder<br>
 ```
 $ cd mini-hacks/kitchen-sync/ios
 ```

4. Download the framework from [here](http://packages.couchbase.com/releases/couchbase-lite/ios/1.0.3.1/couchbase-lite-ios-community_1.0.3.1.zip), upzip, and copy CouchbaseLite.framework to the Frameworks folder.

5. Start sync-gateway by running the following command (Later on you could stop the sync-gateway by running sg.sh script with either `stop` or `clean` option).
 ```
 $ ./script/sg.sh start 
 ```

## Tutorial

1. Open KitchenSync.xcodeproj with your XCode.

2. Open the AppDelegate.h and add the following import statement:<br>
 ```objective-c
 #import <CouchbaseLite/CouchbaseLite.h>
 ```
3. Next, setup a Couchbase Lite database called `kitchen-sync`. Create a new `setupDatabase` method on the AppDelegate.m as follows.

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

4. Create a view named `viewItemsByDate` and setup a map block to index documents by date. Couchbase Lite uses MapReduce queries, which let us create our queries using functions. We can also do powerful transformations of documents, compute aggregates, etc. Add the following code to the end of the `setupDatabase` method.

 ```objective-c
 	[[_database viewNamed: @"viewItemsByDate"] setMapBlock: MAPBLOCK({
		id date = doc[@"created_at"];
        if (date)
            emit(date, doc);
 	}) reduceBlock: nil version: @"1.0"];
 ```

5. Call the `setupDatabase` method from the `application:didFinishLaunchingWithOptions` method as follows.

 ```objective-c
	- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
		// Step 6: Call setupDatabase method
    	[self setupDatabase];
    	...
    	return YES;
	}
 ```

6. We have done all necessary steps to setup the database. Now let's open the `ViewController.m`. Inside the `viewDidLoad` method, get the database object from the AppDelegate as follows.

 ```objective-c
	AppDelegate *app = [[UIApplication sharedApplication] delegate];
	_database = app.database;
 ```

7. Create a new `setupDataSource` method on the `ViewController.m`.

 ```objective-c
	- (void)setupDataSource {

	}
 ```

8. Create a LiveQuery from the `viewItemsByDate` view that we setup in the Step 5.

 ```objective-c
	CBLLiveQuery *query = [[[_database viewNamed:@"viewItemsByDate"] createQuery] asLiveQuery];
	query.descending = YES;
 ```

9. Create a `CBLUITableSource` dataSource object, which basically implements the `UITableViewDataSource` protocol. We configure the `CBLUITableSource` dataSource object with the LiveQuery created from the Step 9. Then, we setup the tableView's dataSource and deletegate object.

 ```objective-c
	_dataSource = [[CBLUITableSource alloc] init];
	_dataSource.query = query;
	_dataSource.tableView = _tableView;

	_tableView.dataSource = _dataSource;
	_tableView.delegate = self;
 ```

10. Call the `setupDataSource` method from the `viewDidLoad` method.

11. To display item documents on the tableview, we implement `couchTableSource:willUseCell:forRow:` method. Basically the `couchTableSource:willUseCell:forRow:` method is called from the `tableView:cellForRowAtIndexPath:` method just before it returns, giving the delegate a chance to customize the new cell. Here we handle displaing text and a check mark.

 ```objective-c
	- (void)couchTableSource:(CBLUITableSource *)source willUseCell:(UITableViewCell *)cell forRow:(CBLQueryRow *)row {
		NSDictionary* rowValue = row.value;
		cell.textLabel.text = rowValue[@"text"];
		BOOL checked = [rowValue[@"check"] boolValue];
		if (checked) {
			cell.textLabel.textColor = [UIColor grayColor];
			cell.accessoryType = UITableViewCellAccessoryCheckmark;
    	} else {
    		cell.textLabel.textColor = [UIColor blackColor];
    		cell.accessoryType = UITableViewCellAccessoryNone;
    	}
	}
 ```

12. Now we need to handle the tableview row touches. Everytime that a row is touched, we toggle the check status of the item associated with the row.

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

13. We have done the document display part but we also need the ability to create a new document from the text field. Add the following `UITextFieldDelegate` methods to the `ViewController.m`.

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
	    NSError* error;
	    if ([doc putProperties: document error: &error]) {
	        textField.text = nil;
	    } else {
	        AppDelegate *app = [[UIApplication sharedApplication] delegate];
	        [app showMessage:@"Couldn't save new item" withTitle:@"Error"];
	    }
	}
 ``` 

14. What about deleting items? Add the following `couchTableSource:deleteRow:` method and use the `CBLUITableSource`'s `deleteDocments:error:` method to delete the document as follows. This will allow us to delete a document when sliding a row in the tableview.

 ```objective-c
	- (bool)couchTableSource:(CBLUITableSource *)source deleteRow:(CBLQueryRow *)row {
	    return [source deleteDocuments:@[row.document] error:nil];
	}
 ```

15. Now is a great time to build and run the application.

16. Let's add sync! Go back to `AppDelegate.m` and Define your sync url location above the implementation of the `AppDelegate` class.

 ```objective-c
 	#define kSyncUrl @"http://<YOUR_WIFI_OR_ETHERNET_IP>:4984/kitchen-sync"
 ```

17. That's the hardest part! Create a new `startSync` method which, in this case, will continuously sync all local and remote changes.

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

18. Observe replication change notification and display network activity indicator when the replicators are active.

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

19. Call the `startSync` method from the `application:didFinishLaunchingWithOptions` method.

 ```objective-c
	- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
		...
		[self startSync];
	    return YES;
	}
 ```

20. Build and run time! When you add a new item or check/uncheck the item, you should see some sync-gateway activities on the console having the sync-gateway running.

21. Finally, let's go see the results of sync in the Sync Gateway Admin Console. Open your browser to [http://localhost:4985/_admin/](http://localhost:4985/_admin/), and click on the [kitchen-sync](http://localhost:4985/_admin/db/kitchen-sync) link. You will land on the **Documents** page, which will list all documents found. Clicking on a document id will reveal the contents of the document.

22. Don't forget to [tell us what you think](https://docs.google.com/forms/d/1Qs9svNccKCC5iji6NXC35uCvdmtFzB0dopz57iApSnY/viewform)!
