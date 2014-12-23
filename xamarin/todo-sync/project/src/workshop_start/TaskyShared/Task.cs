using System;
using System.Collections.Generic;

namespace TaskyShared
{
    public class Task
    {
        public string ID { get; set; }

        public string Name { get; set; }

        public string Notes { get; set; }

        public Dictionary<string, object> ToDictionary()
        {
            var d = new Dictionary<string, object>
            {
                {"name", Name},
                {"notes", Notes}
            };

            return d;
        }
    }
}