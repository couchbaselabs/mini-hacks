using System;
using MonoTouch.Foundation;
using MonoTouch.UIKit;
using TaskyShared;
using System.Collections.Generic;
using System.Linq;

namespace Tasky
{
    partial class HomeScreenController : UITableViewController
    {
        DataSource source;
        bool reloadData = false;

        public HomeScreenController (IntPtr handle) : base (handle)
        {
        }

        public override void ViewDidLoad ()
        {
            base.ViewDidLoad ();

            Title = "Tasks";

            NavigationItem.SetRightBarButtonItem (new UIBarButtonItem (UIBarButtonSystemItem.Add), false);
            NavigationItem.RightBarButtonItem.Clicked += (sender, e) => PerformSegue ("showTaskDetail", this);

            TableView.Source = source = new DataSource (this);
        }

        public override void ViewWillAppear (bool animated)
        {
            base.ViewWillAppear (animated);

            if (reloadData) {
                source.LoadTasks ();
                TableView.ReloadData ();
            }
        }

        class DataSource : UITableViewSource
        {
            static readonly NSString CellIdentifier = new NSString ("TaskCell");
            readonly HomeScreenController controller;
            List<Task> tasks;
            readonly TaskManager taskMgr = new TaskManager ();

            public List<Task> Tasks {
                get {
                    return tasks;
                }
            }

            public DataSource (HomeScreenController controller)
            {
                this.controller = controller;
                LoadTasks ();
            }

            public void LoadTasks ()
            {
                tasks = taskMgr.GetTasks ().ToList ();
            }

            public override int RowsInSection (UITableView tableview, int section)
            {
                return tasks.Count;
            }

            public override UITableViewCell GetCell (UITableView tableView, NSIndexPath indexPath)
            {
                var cell = tableView.DequeueReusableCell (CellIdentifier);
                var task = tasks [indexPath.Row];
                cell.TextLabel.Text = task.Name;
                cell.DetailTextLabel.Text = task.Notes;
                cell.Accessory = task.Done ? UITableViewCellAccessory.Checkmark : UITableViewCellAccessory.None;

                return cell;
            }

            public override void CommitEditingStyle (UITableView tableView, UITableViewCellEditingStyle editingStyle, NSIndexPath indexPath)
            {
                if (editingStyle == UITableViewCellEditingStyle.Delete) {
                    taskMgr.DeleteTask (tasks [indexPath.Row]);
                    tasks.RemoveAt (indexPath.Row);
                    controller.TableView.DeleteRows (new NSIndexPath[] { indexPath }, UITableViewRowAnimation.Fade);   
                }
            }
        }

        public override void PrepareForSegue (UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.Identifier == "showTaskDetail") {
                if (TableView.IndexPathForSelectedRow != null) {

                    var indexPath = TableView.IndexPathForSelectedRow;
                    var task = source.Tasks [indexPath.Row];

                    ((TaskDetailController)segue.DestinationViewController).CurrentTask = task;
                } else {

                    ((TaskDetailController)segue.DestinationViewController).CurrentTask = new Task ();
                }
                reloadData = true;
            }
        }
    }
}