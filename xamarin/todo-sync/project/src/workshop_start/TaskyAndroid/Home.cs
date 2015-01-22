using System.Collections.Generic;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Widget;
using TaskyShared;
using TaskyAndroid;

namespace TaskyAndroid {

	[Activity (Label = "Tasky", MainLauncher = true, Icon="@drawable/icon")]			
	public class Home : Activity {
		TaskListAdapter taskList;
		IList<Task> tasks;
		Button addTaskButton;
		ListView taskListView;

        readonly TaskManager taskMgr = new TaskManager ();
		
		protected override void OnCreate (Bundle bundle)
		{
			base.OnCreate (bundle);

            SetContentView(Resource.Layout.Home);

			taskListView = FindViewById<ListView> (Resource.Id.TaskList);
			addTaskButton = FindViewById<Button> (Resource.Id.AddButton);
           
			if(addTaskButton != null) {
				addTaskButton.Click += (sender, e) => StartActivity (typeof(TaskDetails));
			}

			if(taskListView != null) {
				taskListView.ItemClick += (sender, e) => {
                    var taskDetails = new Intent (this, typeof(TaskDetails));
                    taskDetails.PutExtra ("TaskID", tasks [e.Position].ID);
                    StartActivity (taskDetails);
                };
			}
		}
		
		protected override void OnResume ()
		{
			base.OnResume ();

            tasks = taskMgr.GetTasks();

			taskList = new TaskListAdapter(this, tasks);

			taskListView.Adapter = taskList;
		}
	}
}