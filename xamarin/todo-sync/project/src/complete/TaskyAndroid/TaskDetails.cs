using Android.App;
using Android.Content;
using Android.OS;
using Android.Widget;
using TaskyShared;
using TaskyAndroid;

namespace TaskyAndroid
{

    [Activity (Label = "TaskDetails")]			
    public class TaskDetails : Activity
    {
        Task task = new Task ();
        Button cancelDeleteButton;
        EditText notesTextEdit;
        EditText nameTextEdit;
        Button saveButton;
        CheckBox doneCheckbox;

        readonly TaskManager taskMgr = new TaskManager ();

        protected override void OnCreate (Bundle bundle)
        {
            base.OnCreate (bundle);
			
            string taskID = Intent.GetStringExtra ("TaskID");
            if (!string.IsNullOrEmpty (taskID)) {
                task = taskMgr.GetTask (taskID);
            }
			
            SetContentView (Resource.Layout.TaskDetails);

            nameTextEdit = FindViewById<EditText> (Resource.Id.NameText);
            notesTextEdit = FindViewById<EditText> (Resource.Id.NotesText);
            saveButton = FindViewById<Button> (Resource.Id.SaveButton);
			
            cancelDeleteButton = FindViewById<Button> (Resource.Id.CancelDeleteButton);

            cancelDeleteButton.Text = (string.IsNullOrEmpty (task.ID) ? "Cancel" : "Delete");
			
            nameTextEdit.Text = task.Name; 
            notesTextEdit.Text = task.Notes;

            doneCheckbox = FindViewById<CheckBox>(Resource.Id.chkDone);
            doneCheckbox.Checked = task.Done;

            // button clicks 
            cancelDeleteButton.Click += (sender, e) => CancelDelete ();
            saveButton.Click += (sender, e) => {
                task.Done = doneCheckbox.Checked;
                Save ();
            };
        }

        void Save ()
        {
            task.Name = nameTextEdit.Text;
            task.Notes = notesTextEdit.Text;
            taskMgr.SaveTask (task);
            Finish ();
        }

        void CancelDelete ()
        {
            if (!string.IsNullOrEmpty (task.ID)) {
                taskMgr.DeleteTask (task);
            }
            Finish ();
        }
    }
}