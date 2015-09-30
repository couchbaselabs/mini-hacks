var request = require('request');

request('http://178.62.162.87:4984/ratingapp/_all_docs', function (error, response, body) {
  console.log(body);
  JSON.parse(body).rows.forEach(function (row) {
    request({url: 'http://178.62.162.87:4984/ratingapp/' + row.id + '?rev=' + row.value.rev, method: 'DELETE'}, function (error, response, body) {
      console.log(body);
    });
  });
});
