using System;
using MonoTouch.UIKit;
using TaskyShared;

namespace Tasky
{
    partial class TaskDetailController : UIViewController
    {
        readonly TaskManager taskMgr;

        public Task CurrentTask { get; set; }

        public TaskDetailController (IntPtr handle) : base (handle)
        {
            taskMgr = new TaskManager ();
        }

        public override void ViewDidLoad ()
        {
            base.ViewDidLoad ();

            Title = "Task Details";

            if (CurrentTask != null) {
                taskNameField.Text = CurrentTask.Name;
                taskNotesField.Text = CurrentTask.Notes;
            }
        }

        partial void SaveButton_TouchUpInside (UIButton sender)
        {
            CurrentTask.Name = taskNameField.Text;
            CurrentTask.Notes = taskNotesField.Text;

            taskMgr.SaveTask (CurrentTask);
            NavigationController.PopViewControllerAnimated (true);
        }
    }
}
