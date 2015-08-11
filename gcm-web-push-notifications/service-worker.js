self.addEventListener('push', function (event) {
    console.log('Received a push message', event);

    var notificationOptions = {
        body: 'The highlights of Google I/O 2015',
        icon: 'images/icon@192.png',
        tag: 'highlights-google-io-2015',
        data: null
    };

    if (self.registration.showNotification) {
        self.registration.showNotification('Timely News', notificationOptions);
        return;
    } else {
        new Notification('Timely News', notificationOptions);
    }
});