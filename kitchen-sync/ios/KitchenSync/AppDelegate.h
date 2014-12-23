//
//  AppDelegate.h
//  KitchenSync
//
//  Created by Pasin Suriyentrakorn on 12/22/14.
//  Copyright (c) 2014 Couchbase. All rights reserved.
//

#import <UIKit/UIKit.h>

// Step: 2 Add #import <CouchbaseLite/CouchbaseLite.h>
//#import <CouchbaseLite/CouchbaseLite.h>

@interface AppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;

@property (strong, nonatomic, readonly) CBLDatabase *database;

- (void)showMessage:(NSString *)text withTitle:(NSString *)title;

@end

