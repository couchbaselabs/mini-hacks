var express = require('express');
var bodyParser = require('body-parser');
var app = express();
app.use(bodyParser.json());

var sync = require('./sync');

/**
 * Handle the Sync Gateway webhook request.
 * The request body contains the document.
 */
app.post('/sync_request', function (req, res) {
  var document = req.body;
  console.log('Handle webhook with doc :: %s', JSON.stringify(document));
  sync.syncRequest(document.city);
  res.sendStatus(200);
});

var server = app.listen(8000, function () {
  var host = server.address().address;
  var port = server.address().port;

  console.log('App listening at http://%s:%s', host, port);
});