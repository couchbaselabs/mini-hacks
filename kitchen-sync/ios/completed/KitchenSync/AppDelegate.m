//
//  AppDelegate.m
//  KitchenSync
//
//  Created by Pasin Suriyentrakorn on 12/22/14.
//  Copyright (c) 2014 Couchbase. All rights reserved.
//

#import "AppDelegate.h"

@interface AppDelegate () {
    CBLReplication *_pull;
    CBLReplication *_push;
    NSError *_lastSyncError;
}

@end

// Step 16: Define sync url
#define kSyncUrl @"http://localhost:4984/kitchen-sync"


@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // Step 5: Call setupDatabase method.
    [self setupDatabase];

    // Step 19: Call startSync method.
    [self startSync];

    return YES;
}

// Step 3: Create setupDatabase method and setup a database named 'kitchen-sync'.
- (void)setupDatabase {
    // Step 3: Setup 'kitchen-sync' database
    NSError *error;
    _database = [[CBLManager sharedInstance] databaseNamed:@"kitchen-sync" error:&error];
    if (!_database) {
        NSLog(@"Cannot get kitchen-sync database with error: %@", error);
        return;
    }

    // Step 4: Create a view named "viewItemsByDate" and setup a map block.
    [[_database viewNamed: @"viewItemsByDate"] setMapBlock: MAPBLOCK({
        id date = doc[@"created_at"];
        if (date)
            emit(date, nil);
    }) reduceBlock: nil version: @"1.0"];
}

// Step 17: Create startSync method.
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

// Step 18: Observe replication change notification.
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

- (void)applicationWillResignActive:(UIApplication *)application {
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}

- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

- (void)showMessage:(NSString *)text withTitle:(NSString *)title {
    [[[UIAlertView alloc] initWithTitle:title
                                message:text
                               delegate:self
                      cancelButtonTitle:@"OK"
                      otherButtonTitles:nil] show];
}

@end
