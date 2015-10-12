//: Playground - noun: a place where people can play

import UIKit
import XCPlayground

// Let asynchronous code run
XCPSetExecutionShouldContinueIndefinitely()

let userId = "4567891023456789012340000000000000000000000000"

// 1
let loginURL = NSURL(string: "http://localhost:8000/google_signin")!

// 2
let session = NSURLSession.sharedSession()
let request = NSMutableURLRequest(URL: loginURL)
request.addValue("application/json", forHTTPHeaderField: "Content-Type")
request.HTTPMethod = "POST"

// 3
var properties = [
    "user_id": userId,
    "auth_provider": "google"
]
let data = try! NSJSONSerialization.dataWithJSONObject(properties, options: NSJSONWritingOptions.PrettyPrinted)

// 4
let uploadTask = session.uploadTaskWithRequest(request, fromData: data, completionHandler: { (data, response, error) -> Void in
    // 5
    let json = try! NSJSONSerialization.JSONObjectWithData(data!, options: NSJSONReadingOptions.AllowFragments) as! Dictionary<String, AnyObject>
    print("\(json)")
    
    // TODO: pull/push replications with authenticated user
    
})
uploadTask.resume()
    