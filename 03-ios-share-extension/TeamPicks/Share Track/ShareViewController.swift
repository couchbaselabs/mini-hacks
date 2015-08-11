//
//  ShareViewController.swift
//  Share Track
//
//  Created by James Nocentini on 17/06/2015.
//  Copyright (c) 2015 Couchbase. All rights reserved.
//

import UIKit
import Social

class ShareViewController: SLComposeServiceViewController, TeamViewProtocol {
    
    var item: SLComposeSheetConfigurationItem!
    var teamPickerVC: TeamTableViewController!

    override func isContentValid() -> Bool {
        // Do validation of contentText and/or NSExtensionContext attachments here
        return true
    }

    override func didSelectPost() {
        // This is called after the user selects Post. Do the upload of contentText and/or NSExtensionContext attachments.
    
        var properties = [
            "text": self.contentText,
            "team": self.item.value
        ]
        
        let url = NSURL(string: "http://localhost:4984/db/")!
        let session = NSURLSession.sharedSession()
        
        let request = NSMutableURLRequest(URL: url)
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.HTTPMethod = "POST"
        
        let data = NSJSONSerialization.dataWithJSONObject(properties, options: .allZeros, error: nil)
        
        let uploadTask = session.uploadTaskWithRequest(request, fromData: data) { (data, response, error) -> Void in
            
            // Inform the host that we're done, so it un-blocks its UI. Note: Alternatively you could call super's -didSelectPost, which will similarly complete the extension context.
            self.extensionContext!.completeRequestReturningItems([], completionHandler: nil)
        }
        
        uploadTask.resume()
    }

    override func configurationItems() -> [AnyObject]! {
        self.item = SLComposeSheetConfigurationItem()
        
        self.item.title = "Team"
        self.item.value = "None"
        
        self.item.tapHandler = {
            self.teamPickerVC = TeamTableViewController()
            self.teamPickerVC.delegate = self
            self.pushConfigurationViewController(self.teamPickerVC)
        }
        
        return [self.item]
    }
    
    func sendingViewController(viewController: TeamTableViewController, sentItem: String) {
        self.item.value = sentItem
        self.popConfigurationViewController()
    }

}
