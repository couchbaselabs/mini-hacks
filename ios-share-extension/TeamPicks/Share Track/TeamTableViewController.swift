//
//  TeamTableViewController.swift
//  TeamPicks
//
//  Created by James Nocentini on 17/06/2015.
//  Copyright (c) 2015 Couchbase. All rights reserved.
//

import UIKit

protocol TeamViewProtocol {
    func sendingViewController(viewController: TeamTableViewController, sentItem: String)
}

class TeamTableViewController: UITableViewController {
    
    var teams: [String] = []
    var delegate: TeamViewProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()
        
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
                    
                    self.teams = array
                    
                    dispatch_async(dispatch_get_main_queue(), { () -> Void in
                        self.tableView.reloadData()
                    })
                    
                }
            }
        })
        
        task.resume()
        

        
        self.clearsSelectionOnViewWillAppear = false
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    // MARK: - Table view data source

    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        // #warning Potentially incomplete method implementation.
        // Return the number of sections.
        return 1
    }

    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        // #warning Incomplete method implementation.
        // Return the number of rows in the section.
        return self.teams.count
    }

    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        
        var cell = tableView.dequeueReusableCellWithIdentifier("TeamCell") as? UITableViewCell
        
        if (cell == nil) {
            cell = UITableViewCell(style: .Default, reuseIdentifier: "TeamCell")
        }

        cell!.textLabel!.text = self.teams[indexPath.item]

        return cell!
    }

    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        self.delegate?.sendingViewController(self, sentItem: self.teams[indexPath.item])
    }

}
