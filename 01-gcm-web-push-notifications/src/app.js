var PouchDB = require('pouchdb');

// Expose PouchDB on the window object to use the
// PouchDB Chrome debugger extension http://bit.ly/1L6dArH
window.PouchDB = PouchDB;


var db = new PouchDB('timely-news');
PouchDB.plugin(require('pouchdb-find'));

PouchDB.replicate('timely-news', 'http://localhost:4984/db', {
  live: true
});

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('./service-worker.js', {scope: './'});
  navigator.serviceWorker.ready.then(function (serviceWorkerRegistration) {
    serviceWorkerRegistration.pushManager.subscribe({userVisibleOnly: true})
      .then(function (pushSubscription) {
        console.log('The reg ID is :: ', pushSubscription.subscriptionId);

        db.createIndex({index: {fields: ['type']}})
          .then(function() {
            db.find({
              selector: {type: 'profile'}
            }).then(function (res) {
              console.log(res);
              if (res.docs.length == 0) {
                db.post({
                  'type': 'profile',
                  'registration_ids': [pushSubscription.subscriptionId]
                }, function(err, res) {
                  console.log(err, res);
                });
              }
            });
          });

      });
  });
}