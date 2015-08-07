//: Playground - noun: a place where people can play

import UIKit
import XCPlayground

// Let asynchronous code run
XCPSetExecutionShouldContinueIndefinitely()

var url = NSURL(string: "http://localhost:4984/db/_session")!
var session = NSURLSession.sharedSession()

var task = session.dataTaskWithURL(url, completionHandler: { (data, response, error) -> Void in
    
    var json: Dictionary<String, AnyObject> = (NSJSONSerialization.JSONObjectWithData(data, options: .allZeros, error: nil) as? Dictionary<String, AnyObject>)!

    if let userCtx = json["userCtx"] as? Dictionary<String, AnyObject> {
        if let channels = userCtx["channels"] as? Dictionary<String, AnyObject> {
            
            var array: [String] = []
                
            for (item, _) in channels {
                if item != "!" {
                    array.append(item)
                }
            }
                
        }
    }
})

task.resume()
