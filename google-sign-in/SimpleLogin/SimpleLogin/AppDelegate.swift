//
//  AppDelegate.swift
//  SimpleLogin
//
//  Created by James Nocentini on 11/10/2015.
//  Copyright © 2015 Couchbase. All rights reserved.
//

import UIKit

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate, GIDSignInDelegate {

    var window: UIWindow?
    
    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
        // Initialize sign-in
        var configureError: NSError?
        GGLContext.sharedInstance().configureWithError(&configureError)
        assert(configureError == nil, "Error configuring Google services: \(configureError)")
        
        GIDSignIn.sharedInstance().delegate = self
        
        return true
    }

    func application(application: UIApplication, openURL url: NSURL, sourceApplication: String?, annotation: AnyObject) -> Bool {
        return GIDSignIn.sharedInstance().handleURL(url, sourceApplication: sourceApplication, annotation: annotation)
    }
    
    func signIn(signIn: GIDSignIn!, didSignInForUser user: GIDGoogleUser!, withError error: NSError!) {
        if (error == nil) {
            // Perform any operations on signed in user here.
            let userId = user.userID                  // For client-side use only!
            let name = user.profile.name
            let email = user.profile.email
            print("\(userId), \(name), \(email)")
            // ...
            
            // 1
            let loginURL = NSURL(string: "http://localhost:8000/google_signin")!
            
            // 2
            let session = NSURLSession.sharedSession()
            let request = NSMutableURLRequest(URL: loginURL)
            request.addValue("application/json", forHTTPHeaderField: "Content-Type")
            request.HTTPMethod = "POST"
            
            // 3
            let properties = [
                "user_id": userId,
                "auth_provider": "google"
            ]
            let data = try! NSJSONSerialization.dataWithJSONObject(properties, options: NSJSONWritingOptions.PrettyPrinted)
            
            // 4
            let uploadTask = session.uploadTaskWithRequest(request, fromData: data, completionHandler: { (data, response, error) -> Void in
                // 5
                let json = try! NSJSONSerialization.JSONObjectWithData(data!, options: NSJSONReadingOptions.AllowFragments) as! Dictionary<String, String>
                print("Sync Gateway response --> \(json)")
                
                // TODO: pull/push replications with authenticated user
                self.startReplications(json)
                
            })
            uploadTask.resume()
            
        } else {
            print("\(error.localizedDescription)")
        }
        
        // send http request to app server
        
    }

    func startReplications(sessionInfo: Dictionary<String, String>) {
        // 1
        let dateString = sessionInfo["expires"]!
        let dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        let date = dateFormatter.dateFromString(dateString)!
        
        // 2
        let manager = CBLManager.sharedInstance();
        let database = try! manager.databaseNamed("simple-login")
        
        // 3
        let syncGatewayURL = NSURL(string: "http://localhost:8000/simple-login")!
        let pull = database.createPullReplication(syncGatewayURL)
        pull?.continuous = true
        
        // 4
        pull?.setCookieNamed(sessionInfo["cookie_name"]!, withValue: sessionInfo["session_id"]!, path: "/", expirationDate: date, secure: false)
        pull?.start()
    }

}

