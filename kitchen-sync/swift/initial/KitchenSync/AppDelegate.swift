//
//  AppDelegate.swift
//  KitchenSync
//
//  Created by Pasin Suriyentrakorn on 12/27/14.
//  Copyright (c) 2014 Couchbase. All rights reserved.
//

import UIKit

// Step 16: Define sync url
//private let kSyncUrl = NSURL(string: "http://<YOUR_WIFI_OR_ETHERNET_IP>:4984/kitchen-sync")

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    var database: CBLDatabase!

    private var _pull: CBLReplication!
    private var _push: CBLReplication!
    private var _lastSyncError: NSError?

    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
        // Step 5: Call setupDatabase function

        // Step 19: Call startSync function

        return true
    }

    // Step 3: Create setupDatabase function and setup a database named 'kitchen-sync'.
    /*
    private func setupDatabase() -> Bool {
        // Step 3: Setup 'kitchen-sync' database

        // Step 4: Create a view named 'viewItemsByDate' and setup a map block
    }
    */

    // Step 17: Create startSync function

    // Step 18: Observe relication change notification
    

    func applicationWillResignActive(application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(application: UIApplication) {
        // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }
    
    func showMessage(message: String, title: String) {
        UIAlertView(title: title, message: message, delegate: nil, cancelButtonTitle: "OK").show()
    }
}

