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
    AppDelegate *app = [[UIApplication sharedApplication] delegate];
    _database = app.database;

    // Step 10: Call setupDataSource method.
    [self setupDataSource];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

// Step 7: Create setupDataSource method
- (void)setupDataSource {
// Step 8: Create a LiveQuery from the view named "viewItemsByDate".
    CBLLiveQuery *query = [[[_database viewNamed:@"viewItemsByDate"] createQuery] asLiveQuery];
    query.descending = YES;

// Step 9: Create a CBLUITableSource and link it with the created LiveQuery.
    _dataSource = [[CBLUITableSource alloc] init];
    _dataSource.query = query;
    _dataSource.tableView = _tableView;
    
    _tableView.dataSource = _dataSource;
    _tableView.delegate = self;

}

#pragma mark CBLUITableSource

// Step 11: Setup tableview display.
- (void)couchTableSource:(CBLUITableSource *)source willUseCell:(UITableViewCell *)cell forRow:(CBLQueryRow *)row {
    NSDictionary *properties = row.document.properties;
    cell.textLabel.text = properties[@"text"];
    BOOL checked = [properties[@"check"] boolValue];
    if (checked) {
        cell.textLabel.textColor = [UIColor grayColor];
        cell.accessoryType = UITableViewCellAccessoryCheckmark;
    } else {
        cell.textLabel.textColor = [UIColor blackColor];
        cell.accessoryType = UITableViewCellAccessoryNone;
    }
}

// Step 14: Delete a document.
- (bool)couchTableSource:(CBLUITableSource *)source deleteRow:(CBLQueryRow *)row {
    NSError *error;
    return [row.document deleteDocument:&error];
}

#pragma mark UITableViewDelegate

// Step 12: Setup tableview row selection.
- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    CBLQueryRow *row = [_dataSource rowAtIndex:indexPath.row];
    CBLDocument *doc = row.document;
    
    NSError *error;
    CBLSavedRevision *newRev = [doc update:^BOOL(CBLUnsavedRevision *rev) {
        // Toggle the "check" property of the new revision to be saved:
        BOOL wasChecked = [rev[@"check"] boolValue];
        rev[@"check"] = @(!wasChecked);
        return YES;
    } error:&error];
    
    if (!newRev) {
        AppDelegate *app = [[UIApplication sharedApplication] delegate];
        [app showMessage:@"Failed to update item" withTitle:@"Error"];
    }
}

#pragma mark UITextFieldDelegate

// Step 13: Create a new document after entering data.
-(BOOL)textFieldShouldReturn:(UITextField *)textField {
    [textField resignFirstResponder];
    return (textField.text.length > 0);
}

-(void)textFieldDidEndEditing:(UITextField *)textField {
    if (textField.text.length == 0) return;
    
    // Create the new document's properties:
    NSDictionary *document = @{@"text": textField.text,
                               @"check": @NO,
                               @"created_at": [CBLJSON JSONObjectWithDate: [NSDate date]]};
    
    // Save the document:
    CBLDocument *doc = [_database createDocument];
    NSError *error;
    if ([doc putProperties: document error: &error]) {
        textField.text = nil;
    } else {
        AppDelegate *app = [[UIApplication sharedApplication] delegate];
        [app showMessage:@"Couldn't save new item" withTitle:@"Error"];
    }
}

@end
