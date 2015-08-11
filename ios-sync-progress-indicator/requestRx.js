const rx = require('rx');
const request = require('request');

let wrapMethodInRx = (method) => {
    return function (...args) {
        return rx.Observable.create((subj) => {
            // Push the callback as the last parameter
            args.push((err, resp, body) => {
                if (err) {
                    subj.onError(err);
                    return;
                }

                if (resp.statusCode >= 400) {
                    subj.onError(new Error(`Request failed: ${resp.statusCode}\n${body}`));
                    return;
                }

                subj.onNext({response: resp, body: body});
                subj.onCompleted();
            });

            try {
                method(...args);
            } catch (e) {
                subj.onError(e);
            }

            return rx.Disposable.empty;
        })
    }
};

let requestRx = wrapMethodInRx(request);
requestRx.get = wrapMethodInRx(request.get);
requestRx.post = wrapMethodInRx(request.post);
requestRx.patch = wrapMethodInRx(request.patch);
requestRx.put = wrapMethodInRx(request.put);
requestRx.del = wrapMethodInRx(request.del);

requestRx.pipe = (url, stream) => {
    return rx.Observable.create((subj) => {
        try {
            request.get(url).pipe(stream)
              .on('error', (err) => subj.onError(err))
              .on('end', () => { subj.onNext(true); subj.onCompleted(); });
        } catch (e) {
            subj.onError(e);
        }
    });
};

module.exports = requestRx;