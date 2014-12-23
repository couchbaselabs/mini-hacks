#Xamarin CouchbaseLite Workshop

NOTE: The **src** folder contains a **workshop_start** folder containing a solution to serve as a starting point for the workshop. There is also a **complete** folder containing the finished solution.

##Xamarin.iOS
###Running the Sample Application

First, let's open the Tasky sample application. Tasky is a simple To-Do application that you can use to track tasks. It's a cross-platform sample that shares its core code between iOS and Android.

The solution and project structure in the Solution Pad shows all the files in the solution and should look familiar to any modern IDE user.

If you cannot see the Solution Pad, choose View → Pads → Solution from the menu.

![solution pad](/images/solutionpad.png)

Check that the Debug configuration is selected in the toolbar and choose Run → Debug from the menu or press Command + Return (or the 'play' button) to start debugging with the iOS Simulator:

![simulator](/images/taskysim.png)

The app looks pretty empty to start with – press the plus button and add a few tasks to see how it works.

Before looking at the code more closely, let's revisit the solution and application architecture. There are two projects, which help facilitate a cross-platform solution when combined with Xamarin.Android:

###TaskyIOS

An iOS application project containing the user-interface and application layer for iPhones & iPads.

####TaskyShared

A shared project that contains platform-independent code. This code is shared between different device platforms, such as Android and iOS. When referenced from the platform-specific project it behaves as if the files were included directly in the referencing project.

####Understanding the Architecture

The architecture and project structure of the Tasky application is shown in the following diagram:

![architecture](/images/architecture.png)

- User Interface - The screens, controls and data presentation code. In Xamarin.iOS these classes are wrappers around the iOS CocoaTouch frameworks. The user interface that you build looks, feels and performs like a native Objective-C application.
- App Layer - Custom classes to bind the business layer to the user interface, typically requiring platform specific features.
- Business Layer - Business object classes and business logic.
- Data Access Layer - Abstraction layer between the business logic and the data layer.
- Data Layer - Low-level data persistence and retrieval; the Tasky sample uses Couchbase Lite. 

####Modifying Tasky

As it sits, Tasky lacks a basic feature of any decent task application: the ability to mark a task complete. We're going to add that functionality by making the following modifications:

1. Implement a new property on the Task object called Done.

2. Add a mechanism in the UI to allow users to mark a task as completed.

3. Change the home screen to display whether a task is completed.

4. Allow deleting a task.

![task detail](/images/detail.png)

#####Implement a new property on the Task class

Open the Task.cs file in **TaskyShared** and notice the class has the following property:

	public bool Done { get; set; }
	
#####Update the Couchbase Lite code

Tasky uses the _Couchbase Lite_ Xamarin component to store and retrieve data. All data operations can be performed with Couchbase Lite. Here's an example of how Couchbase Lite can be used to access data stored on a device:

	public IList<Task> GetTasks ()
	{
	    var query = db.CreateAllDocumentsQuery ();
	    var results = query.Run ();
	    var tasks = new List<Task> ();
	
	    foreach (var row in results) {
	        var task = new Task {
	            ID = row.DocumentId,
	            Name = row.Document.UserProperties ["name"].ToString (),
	            Notes = row.Document.UserProperties ["notes"].ToString ()
	        };
	        tasks.Add (task);
	    }
	    return tasks;
	}
	
The Couchbase Lite code is implemented in the TaskManager class in TaskyShared. Update this class to account for the newly added Done property.

Now we need to modify the UI to support this change.

#####Add corresponding controls to the Task Details screen in the storyboard

Now that we've modified our Task object, let's modify the user interface to allow users to mark them as complete and also to delete a task.

Tasky uses the Xamarin iOS designer to create the user interface, contained in the HomeScreen.storyboard file.

We can drag and drop a Switch and Label to change the Done property, and a Button to delete a task, giving each control a Name in Property Pad.

![storyboard](/images/storyboard.png)

#####Sync changes made in the user interface back to the business object

First let's implement delete. If we double-click on the button, an event handler for the TouchUpInside event is generated in the TaskDetailController class:

    partial void DeleteButton_TouchUpInside (UIButton sender)
    {
	    if (CurrentTask.ID != null) {
		    taskMgr.DeleteTask (CurrentTask);
	    }
	
	    NavigationController.PopViewControllerAnimated (true);
    }
    
We also want users to be able to swipe to delete task in the UITableView of the HomeScreenController. We can accomplish this by implementing CommitEditingStyle as follows:

	public override void CommitEditingStyle (UITableView tableView,
	    UITableViewCellEditingStyle editingStyle, NSIndexPath indexPath)
	{
	    if (editingStyle == UITableViewCellEditingStyle.Delete) {
	        taskMgr.DeleteTask (tasks [indexPath.Row]);
	        tasks.RemoveAt (indexPath.Row);
	        
	        controller.TableView.DeleteRows (
		        new NSIndexPath[] { indexPath }, UITableViewRowAnimation.Fade);   
	    }
	}

Back in the TaskDetailController, to persist the Done property, we simply access the value from the Switch by the name we gave the control in the Property Pad and set the value of the Task object:

    partial void SaveButton_TouchUpInside (UIButton sender)
    {
      CurrentTask.Name = taskNameField.Text;
      CurrentTask.Notes = taskNotesField.Text;
      CurrentTask.Done = taskDoneSwitch.On;

      taskMgr.SaveTask (CurrentTask);

      NavigationController.PopViewControllerAnimated (true);
    }

Also, we can set the initial value of the Switch when the view loads:

    public override void ViewDidLoad ()
    {
      base.ViewDidLoad ();

      Title = "Task Details";

      if (CurrentTask != null) {
        taskNameField.Text = CurrentTask.Name;
        taskNotesField.Text = CurrentTask.Notes;
        taskDoneSwitch.On = CurrentTask.Done;
      }
    }

#####Alter the Home screen so that the Done status is displayed in the list

To complete our new feature, we need to display the completion status of each task on the home screen.

We can do this by simply updating the cell's Accessory in the DataSource's GetCell method in HomeScreenController.cs, based on the value of the task's Done property:

	cell.Accessory = task.Done ? UITableViewCellAccessory.Checkmark : UITableViewCellAccessory.None;

![cellaccessory](/images/cellaccessory.png)

We've now created our first application in Xamarin.iOS using Couchbase Lite. We've seen Xamarin Studio and we built and tested an application in the simulator.

---

##Xamarin.Android

###Running the Sample Application

First, let's open the Tasky sample application. Tasky is a simple To-Do application that you can use to track tasks. It's a cross-platform sample that shares its core code between iOS and Android.

The solution and project structure in the Solution Pad shows all the files in the solution and should look familiar to any modern IDE user.

If you cannot see the Solution Pad, choose View → Pads → Solution from the menu.

![solution pad](/images/solutionpad.png)

Check that the Debug configuration is selected in the toolbar and choose Run → Debug from the menu or press Command + Return (or the 'play' button) to start debugging with an Android Emulator. Here's Tasky running in the Xamarin Android Player:

![Tasky Android Player](/images/tasky_xap.png)

Before looking at the code more closely, let’s review the solution and application architecture. There are two projects, which help facilitate a cross-platform solution when combined with Xamarin.Android:

- TaskyAndroid - An Android application project containing the user-interface and application layer for Android devices.
- TaskyShared - A shared project that contains platform-independent code. This code is shared between different device platforms, such as Android and iOS. When referenced from the platform-specific project it behaves as if the files were included directly in the referencing project.

####Understanding the Architecture

The architecture and project structure of the Tasky application is shown in the following diagram:

![architecture](/images/architecture.png)

- User Interface - The screens, controls and data presentation code. In Xamarin.iOS these classes are wrappers around the iOS CocoaTouch frameworks. The user interface that you build looks, feels and performs like a native Objective-C application.
- App Layer - Custom classes to bind the business layer to the user interface, typically requiring platform specific features.
- Business Layer - Business object classes and business logic.
- Data Access Layer - Abstraction layer between the business logic and the data layer.
- Data Layer - Low-level data persistence and retrieval; the Tasky sample uses Couchbase Lite.

####Modifying Tasky

As it sits, Tasky lacks a basic feature of any decent task application: the ability to mark a task complete. We’re going to add that functionality by making the following modifications:

1. Implement a new property on the Task object called Done.
2. Add a mechanism in the UI to allow users to mark a task as completed.
3. Change the home screen to display whether a task is completed.

#####Implement a new property on the Task class

Open the Task.cs file in **TaskyShared** and add the following property:

	public bool Done { get; set; }
	
#####Update the Couchbase Lite code

Tasky uses the _Couchbase Lite_ Xamarin component to store and retrieve data. All data operations can be performed with Couchbase Lite. Here's an example of how Couchbase Lite can be used to access data stored on a device:

	public IList<Task> GetTasks ()
	{
	    var query = db.CreateAllDocumentsQuery ();
	    var results = query.Run ();
	    var tasks = new List<Task> ();
	
	    foreach (var row in results) {
	        var task = new Task {
	            ID = row.DocumentId,
	            Name = row.Document.UserProperties ["name"].ToString (),
	            Notes = row.Document.UserProperties ["notes"].ToString ()
	        };
	        tasks.Add (task);
	    }
	    return tasks;
	}
	
The Couchbase Lite code is implemented in the TaskManager class in TaskyShared. Update this class to account for the newly added Done property.

Now we need to modify the UI to support this change.

#####Add a corresponding property to the Task Details screen in the Application Layer

Now that we’ve modified our Task object, let’s modify the user interface to allow users to mark them as complete.

Screens in Xamarin.Android are typically defined by an XML layout file (with the .axml suffix) and one or more C# subclasses of Android.App.Activity that load that layout. We need to modify both the AXML and the C# to add the Done property to our user interface.

The Task Details layout is defined in the TaskDetails.axml file in the Resources/Layout folder. It already contains controls for the Task Name and Notes text inputs and labels, plus two buttons to Save or Delete the Task. To display the Task’s completion status we will add a Checkbox to this layout with the following XML after the NotesText control (which includes re-defining the layout_below property of both buttons):

	<CheckBox
	    android:id="@+id/chkDone"
	    android:layout_below="@+id/NotesText"
	    android:layout_width="wrap_content"
	    android:layout_height="wrap_content"
	    android:text="Done" />
	<Button
	    android:id="@+id/SaveButton"
	    android:text="Save"
	    android:layout_width="fill_parent"
	    android:layout_height="wrap_content"
	    android:layout_below="@+id/chkDone" />
	<Button
	    android:id="@+id/CancelDeleteButton"
	    android:text="Cancel"
	    android:layout_width="fill_parent"
	    android:layout_height="wrap_content"
	    android:layout_below="@+id/SaveButton" />

The Task Details screen will now render a checkbox under the Notes input.

#####Sync changes made in the UI back to the business object

Displaying the Done checkbox control is only half of the data binding job - we must also load the current task’s Done status to set the control, and save the value if it changes. The Task Details Activity subclass is defined in the TaskDetails.cs file.

The following changes are required to hook up the checkbox to the business object:

- Define a new field in the class:
```
CheckBox doneCheckbox;
```
- Find the checkbox control and set its value in the OnCreate method:
```
doneCheckbox = FindViewById<CheckBox>(Resource.Id.chkDone);
doneCheckbox.Checked = task.Done;
```
- Retrieve the value before persisting in the Save method:
```
task.Done = doneCheckbox.Checked;
```
The updated task business object is then saved to the database via the Business Layer’s TaskManager class. This completes the changes to the Task Details screen - it will now display and save the new property we added to our business object.

#####Alter the Home screen so that the Done status is displayed in the list

The final step is to display the completion status of each task on the home screen.

Each row in the task list is rendered via calls to the GetView method of the TaskListAdapter class. We need to alter the code to use a CheckedTextView and then set the Checked property. To do this replace the custom TaskListItem layout with the SimpleListItemCheck layout:

	var view = (convertView ??
	    context.LayoutInflater.Inflate(
	    Android.Resource.Layout.SimpleListItemChecked,
	    parent,
	    false)) as CheckedTextView;

As we have changed the layout, delete the code describing the old Task List and replace it with code that matches the CheckedTextView properties.

	view.SetText (item.Name==""?"<new task>":item.Name, TextView.BufferType.Normal);
	view.Checked = item.Done;
	
The application now looks like this – the task completion status is visible in the list and can be marked as Done on the Task Details screen using the checkbox control:

![task done](/images/taskdone_xap.png)

We've now created our first application in Xamarin.Android using Couchbase Lite. We've seen Xamarin Studio and we built and tested an application in the emulator.
