var express = require('express')
  , bodyParser = require('body-parser')
  , request = require('request').defaults({json: true})
  , httpProxy = require('http-proxy');

// 1
var app = express();
app.use('/signup', bodyParser.json());

// 2
app.post('/signup', function (req, res) {
  console.log('its signup time');

  var json = req.body;
  var options = {
    url: 'http://0.0.0.0:4985/smarthome/_user/',
    method: 'POST',
    body: json
  };

  request(options, function(error, response) {
    res.writeHead(response.statusCode);
    res.end();
  });
});

// 3
app.all('*', function(req, res) {
  var url = 'http://0.0.0.0:4984' + req.url;
  req.pipe(request(url)).pipe(res);
});

// 4
var server = app.listen(8000, function () {
  var host = server.address().address;
  var port = server.address().port;

  console.log('App listening at http://%s:%s', host, port);
});