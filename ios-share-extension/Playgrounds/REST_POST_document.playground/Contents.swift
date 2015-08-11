//: Playground - noun: a place where people can play

import UIKit
import XCPlayground

// Let asynchronous code run
XCPSetExecutionShouldContinueIndefinitely()

let url = NSURL(string: "http://localhost:4984/db/")!
let session = NSURLSession.sharedSession()

let request = NSMutableURLRequest(URL: url)
request.addValue("application/json", forHTTPHeaderField: "Content-Type")
request.HTTPMethod = "POST"

let json: Dictionary<String, AnyObject> = ["name": "oliver"]
let data = NSJSONSerialization.dataWithJSONObject(json, options: NSJSONWritingOptions.allZeros, error: nil)

let uploadTask = session.uploadTaskWithRequest(request, fromData: data) { (data, response, error) -> Void in
    let json = NSJSONSerialization.JSONObjectWithData(data, options: NSJSONReadingOptions.MutableLeaves, error: nil) as! Dictionary<String, AnyObject>
    println(json)
    
}

uploadTask.resume()
