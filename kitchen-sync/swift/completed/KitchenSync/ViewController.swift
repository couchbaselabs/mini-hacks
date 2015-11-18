//
//  ViewController.swift
//  KitchenSync
//
//  Created by Pasin Suriyentrakorn on 12/27/14.
//  Copyright (c) 2014 Couchbase. All rights reserved.
//

import UIKit

class ViewController: UIViewController, UITextFieldDelegate, CBLUITableDelegate {

    @IBOutlet weak var textField: UITextField!

    @IBOutlet weak var tableView: UITableView!
    
    private var _database: CBLDatabase!
    private var _dataSource: CBLUITableSource!

    override func viewDidLoad() {
        super.viewDidLoad()

        // Step 6: Get the dtabase object.
        let app = UIApplication.sharedApplication().delegate as! AppDelegate
        _database = app.database

        // Step 10: Call setupDataSource function.
        setupDataSource()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    // Step 7: Create setupDataSource function.
    private func setupDataSource() {
        // Step 8: Create a LiveQuery from the view named "viewItemsByDate".
        let query = _database.viewNamed("viewItemsByDate").createQuery().asLiveQuery()
        query.descending = true

        // Step 9: Create a CBLUITableSource and link it with the created LiveQuery.
        _dataSource = CBLUITableSource()
        _dataSource.query = query
        _dataSource.tableView = tableView
        
        tableView.dataSource = _dataSource
        tableView.delegate = self
    }

    // MARK: - CBLUITableSource

    // Step 11: Setup tableview display.
    func couchTableSource(source: CBLUITableSource, willUseCell cell: UITableViewCell, forRow row: CBLQueryRow) {
        let properties = row.document!.properties
        cell.textLabel!.text = properties!["text"] as? String
        
        let checked = (properties!["check"] as? Bool) ?? false
        if checked {
            cell.textLabel!.textColor = UIColor.grayColor()
            cell.accessoryType = UITableViewCellAccessoryType.Checkmark
        } else {
            cell.textLabel!.textColor = UIColor.blackColor()
            cell.accessoryType = UITableViewCellAccessoryType.None
        }
    }

    // Step 14: Delete a document.
    func couchTableSource(source: CBLUITableSource, deleteRow row: CBLQueryRow) -> Bool {
        do {
            try row.document!.deleteDocument()
            return true;
        } catch {
            return false;
        }
    }

    // MARK: - UITableViewDelegate

    // Step 12: Setup tableview row selection.
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let row = _dataSource.rowAtIndex(UInt(indexPath.row))
        
        do {
            try row!.document!.update({ (rev: CBLUnsavedRevision!) -> Bool in
                let wasChecked = (rev["check"] as? Bool) ?? false
                rev.properties!["check"] = !wasChecked
                return true
            })
        } catch {
            let app = UIApplication.sharedApplication().delegate as! AppDelegate
            app.showMessage("Failed to update item", title: "Error")
        }
    }
    
    // MARK: - UITextFieldDelegate

    // Step 13: Create a new document after entering data.
    func textFieldShouldReturn(textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return !textField.text!.isEmpty
    }
    
    func textFieldDidEndEditing(textField: UITextField) {
        if textField.text!.isEmpty {
            return
        }
        
        // Create the new document's properties:
        let properties: Dictionary<String,AnyObject> = [
            "text": textField.text!,
            "check": false,
            "created_at": CBLJSON.JSONObjectWithDate(NSDate(), timeZone: NSTimeZone.localTimeZone())
        ]
        
        // Save the document:
        let doc = _database.createDocument()
        
        do {
            try doc.putProperties(properties)
            textField.text = nil
        } catch {
            let app = UIApplication.sharedApplication().delegate as! AppDelegate
            app.showMessage("Couldn't save new item", title: "Error")
        }
    }
}

