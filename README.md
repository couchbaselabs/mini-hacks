# Sync NoSQL Data With PouchDB, AngularJS, and Couchbase

This is a sample project to demonstrate how you can use Couchbase as your database using only AngularJS and JavaScript in your web application.

## Instructions

Download the latest master branch commit as well as the latest version of the Couchbase Sync Gateway.  From your Terminal or Command Prompt, with the project as your current working directory, run:

```
python -m SimpleHTTPServer 9000
```

This will start serving the web application using a lightweight Python server.  If you don't serve the application and try to run it by opening the HTML file in your web browser you'll get cross origin resource sharing (CORS) related issues.

With the Couchbase Sync Gateway downloaded, from the Terminal or Command Prompt, start the Sync Gateway by running:

```
/path/to/sync/gateway/bin/sync_gateway /path/to/project/sync-gateway-config.json
```

You can now test the application by visiting **http://localhost:9000** from your web browser.

## Resources

PouchDB - [http://www.pouchdb.com](http://www.pouchdb.com)

AngularJS - [http://www.angularjs.org](http://www.angularjs.org)

Couchbase - [http://www.couchbase.com](http://www.couchbase.com)
