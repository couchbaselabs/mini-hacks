using System.Collections.Generic;
using Android.App;
using Android.Widget;
using TaskyShared;

namespace TaskyAndroid {

	public class TaskListAdapter : BaseAdapter<Task> {
		Activity context = null;
		IList<Task> tasks = new List<Task>();
		
		public TaskListAdapter (Activity context, IList<Task> tasks) : base ()
		{
			this.context = context;
			this.tasks = tasks;
		}
		
		public override Task this[int position]
		{
			get { return tasks[position]; }
		}
		
		public override long GetItemId (int position)
		{
			return position;
		}
		
		public override int Count
		{
			get { return tasks.Count; }
		}
		
		public override Android.Views.View GetView (int position, Android.Views.View convertView, Android.Views.ViewGroup parent)
		{
			// Get our object for position
			var item = tasks[position];			

			//Try to reuse convertView if it's not  null, otherwise inflate it from our item layout
			// gives us some performance gains by not always inflating a new view
			// will sound familiar to MonoTouch developers with UITableViewCell.DequeueReusableCell()
			var view = (convertView ?? 
					context.LayoutInflater.Inflate(
					Resource.Layout.TaskListItem, 
					parent, 
					false)) as LinearLayout;

			// Find references to each subview in the list item's view
            var nameText = view.FindViewById<TextView>(Resource.Id.NameText);
            var decsriptionText = view.FindViewById<TextView>(Resource.Id.NotesText);

			//Assign item's values to the various subviews
			nameText.SetText (item.Name, TextView.BufferType.Normal);
			decsriptionText.SetText (item.Notes, TextView.BufferType.Normal);

			//Finally return the view
			return view;
		}
	}
}