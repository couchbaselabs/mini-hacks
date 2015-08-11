var requestRx = require('./requestRx.js');
var Rx = require('rx');

const api_key = 'AIzaSyD4e6ZUIc9G2AxKansIUKa0enFzWZy5h8w';
const url = 'https://maps.googleapis.com/maps/api/place';
const gateway = 'http://localhost:4985/db';

// 1. Search for Places
requestRx.get(`${url}/textsearch/json?key=${api_key}&query=restaurants+in+london`)
  .subscribe((res) => {
      var places = JSON.parse(res.body).results;
      var placesStream = Rx.Observable.fromArray(places);

      // 2. Send the Places in bulk to Sync Gateway
      requestRx({uri: `${gateway}/_bulk_docs`, method: 'POST', json: {docs: places}})
        .flatMap((docsRes) => {
            var docsStream = Rx.Observable.fromArray(docsRes.body);

            // Merge the place's photoreference with the doc id and rev
            return Rx.Observable.zip(placesStream, docsStream, (place, doc) => {
                return {
                    id: doc.id,
                    rev: doc.rev,
                    ref: place.photos[0].photo_reference
                }
            });
        })
        .flatMap((doc) => {

            // 3. Get the binary jpg photo using the ref property (i.e. photoreference)
            var options = {
                uri: `${url}/photo?key=${api_key}&maxwidth=400&photoreference=${doc.ref}`,
                encoding: null
            };
            return requestRx.get(options)
              .flatMap((photo) => {

                  // 4. Save the photo as an attachment on the corresponding document
                  return requestRx({
                      uri: `${gateway}/${doc.id}/photo?rev=${doc.rev}`,
                      method: 'PUT',
                      headers: {'Content-Type': 'image/jpg'},
                      body: photo.body
                  })
              })
        })
        .subscribe((res) => {
        });
  });