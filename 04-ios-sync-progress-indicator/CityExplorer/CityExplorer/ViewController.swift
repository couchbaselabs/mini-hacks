//
//  ViewController.swift
//  CityExplorer
//
//  Created by James Nocentini on 21/06/2015.
//  Copyright (c) 2015 Couchbase. All rights reserved.
//

import UIKit

class ViewController: UIViewController {
    
    var pull: CBLReplication?

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        let manager = CBLManager.sharedInstance()
        let databaseExists = manager.databaseExistsNamed("cityexplorer")
        var database = manager.databaseNamed("cityexplorer", error: nil)
        if databaseExists {
            database?.deleteDatabase(nil)
            database = manager.databaseNamed("cityexplorer", error: nil)
        }
        
        let gateway = NSURL(string: "http://localhost:4984/db")!
        
        pull = database?.createPullReplication(gateway)
        
        let nctr = NSNotificationCenter.defaultCenter()
        nctr.addObserver(self, selector: "replicationProgress:", name: kCBLReplicationChangeNotification, object: pull)
        
        pull?.start()
    }
    
    @IBOutlet var progressView: UIProgressView!
    
    func replicationProgress(notification: NSNotification) {
        
        let active = pull?.status == .Active
        let completed = pull?.status == .Stopped
        
        println("Status : \(pull?.status.rawValue)")
        println("Changes Count: \(pull?.changesCount)")
        println("Completed Count: \(pull?.completedChangesCount)")
        println("======")
        
        if pull!.changesCount > 0 {
            let number = Float(pull!.completedChangesCount) / Float(pull!.changesCount)
            self.progressView.progress = number
        }

    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }


}

