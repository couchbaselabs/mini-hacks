//
//  ViewController.m
//  KitchenSync
//
//  Created by Pasin Suriyentrakorn on 12/22/14.
//  Copyright (c) 2014 Couchbase. All rights reserved.
//

#import "ViewController.h"
#import "AppDelegate.h"

@interface ViewController () <CBLUITableDelegate, UITextFieldDelegate> {
    CBLDatabase *_database;
    CBLUITableSource *_dataSource;
}

@property (weak, nonatomic) IBOutlet UITextField *textField;
@property (weak, nonatomic) IBOutlet UITableView *tableView;

@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];

    // Step 6: Get the database object.

    // Step 10: Call setupDataSource method.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

// Step 7: Create setupDataSource method
/*
- (void)setupDataSource {
// Step 8: Create a LiveQuery from the view named "viewItemsByDate".

// Step 9: Create a CBLUITableSource and link it with the created LiveQuery.

}
*/

#pragma mark CBLUITableSource

// Step 11: Setup tableview display.

// Step 14: Delete a document.

#pragma mark UITableViewDelegate

// Step 12: Setup tableview row selection.

#pragma mark UITextFieldDelegate

// Step 13: Create a new document after entering data.

@end
