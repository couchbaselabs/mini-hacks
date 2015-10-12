var express = require('express')
  , bodyParser = require('body-parser')
  , request = require('request').defaults({json: true})
  , google = require('googleapis');

var app = express();
app.use('/google_signin', bodyParser.json());

app.post('/google_signin', function (req, res) {
  /** URL of the Sync Gateway instance running locally */
  var stringURL = 'http://0.0.0.0:4985/simple-login';

  /** Given the name of a user that exists in Sync Gateway, create a new session */
  var sessionRequest = function (name, callback) {
    return request({
      method: 'POST',
      url: stringURL + '/_session',
      json: true,
      body: {
        name: name
      }
    }, callback);
  };

  var json = req.body;
  var name = json.auth_provider + '-' + json.user_id.toString();
  request
    /** Check if the user already exists */
    .get(stringURL + '/_user/' + name)
    .on('response', function (userExistsResponse) {
      if (userExistsResponse.statusCode === 404) {
        /** If the user doesn't exist, create one with the Google user ID as the name */
        return request
          .put({
            url: stringURL + '/_user/' + name,
            json: true,
            body: {
              name: name,
              password: Math.random.toString(36).substr(2)
            }
          })
          .on('response', function (createUserResponse) {
            if (createUserResponse.statusCode === 201) {
              /** If the user was created successfully, create the session */
              sessionRequest(name, function (sessionError, sessionResponse, body) {
                res.send(body);
              });
            }
          });
      }
      /** The user already exists, simply create a new session */
      sessionRequest(name, function (sessionError, sessionResponse, body) {
        res.send(body);
      });
    });
});

app.all('*', function (req, res) {
  var url = 'http://0.0.0.0:4984' + req.url;
  req.pipe(request(url)).pipe(res);
});

var server = app.listen(8000, function () {
  var host = server.address().address;
  var port = server.address().port;

  console.log('App listening at http://%s:%s', host, port);
});