var http = require('http')
  , httpProxy = require('http-proxy')
  , request = require('request').defaults({json: true});

var proxy = httpProxy.createProxyServer();
var server = http.createServer(function (req, res) {

  if (/signup.*/.test(req.url)) {
    console.log('its signup time');

    req.on('data', function (chunk) {
      var json = JSON.parse(chunk);
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

    req.on('end', function () {

    });

  } else {
    proxy.web(req, res, {target: 'http://0.0.0.0:4984'});
  }

});

server.listen(8000);