//workshop version of TaskManager.cs

using System;
using System.Collections.Generic;
//1.  Add the Couchbase Lite using statement  

namespace TaskyShared  
{
    public class TaskManager
    {
        //2.  Declare a 'Database' variable 

        public TaskManager ()
        {
            //3. Initialize the database   
        }

        //4.  Create a GetTask method
        public Task GetTask (string id)
        {
            var doc = //5.  Obtain the document of interest 
            var props = //6. Obtain the document properties 

            var task = new Task {
                ID = id,
                Name = props ["name"].ToString (),
                Notes = props ["notes"].ToString (),
            };

            //7. Return the object of interest  
        }

     
        public IList<Task> GetTasks ()
        {
            var query = //8.  Obtain all the documents in the database  
            var results = query.Run ();
            var tasks = new List<Task> ();

            foreach (var row in results) {
                var task = new Task {
                    //9.  Return the Document ID 
                    Name = row.Document.UserProperties ["name"].ToString (),
                    //10.  Return the value in 'notes' 
                };
                tasks.Add (task);
            }
            return tasks;
        }

   
        public void SaveTask (Task item)
        {
            Document doc;

            if (item.ID == null) {
                doc = //11.  Create Document 
                //12. Add Task Item to Document 
                item.ID = doc.Id;
				Console.WriteLine("You have created a new Document: " + doc);
				Console.WriteLine("The Document ID is: " + item.ID);
            } else {
                doc = //13. Retrieve Document 
				Console.WriteLine("The previous revision " + item.ID);
                //14.  Update Document 

            }
        }

        public void DeleteTask (Task task)
        {
            var doc = db.GetDocument (task.ID);
            //15.  Delete Document 
			Console.WriteLine("You have deleted: " + doc.Id);
        }
    }
}