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

        // Step 10: Call setupDataSource function.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    // Step 7: Create setupDataSource function.
    /*
    private func setupDataSource() {
        // Step 8: Create a LiveQuery from the view named "viewItemsByDate".

        // Step 9: Create a CBLUITableSource and link it with the created LiveQuery.

    }
    */

    // MARK: - CBLUITableSource

    // Step 11: Setup tableview display.

    // Step 14: Delete a document.


    // MARK: - UITableViewDelegate

    // Step 12: Setup tableview row selection.
    
    // MARK: - UITextFieldDelegate

    // Step 13: Create a new document after entering data.

}

