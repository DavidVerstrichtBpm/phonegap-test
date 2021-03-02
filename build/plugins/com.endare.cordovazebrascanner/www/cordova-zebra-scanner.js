var exec = require('cordova/exec');

exports.testScan = function (shouldSucceed, onSuccess, onError) {
    exec(onSuccess, onError, 'cordovaZebraScanner', 'testScan', [shouldSucceed]);
};

exports.scanOnce = function (onSuccess, onError) {
    exec(onSuccess, onError, 'cordovaZebraScanner', 'scanOnce', []);
};

exports.startScanning = function (onSuccess, onError) {
    exec(onSuccess, onError, 'cordovaZebraScanner', 'startScanning', []);
};

exports.stopScanning = function (onSuccess, onError) {
    exec(onSuccess, onError, 'cordovaZebraScanner', 'stopScanning', []);
};

exports.fakeScan = function (barcode, onSuccess, onError) {
    exec(onSuccess, onError, 'cordovaZebraScanner', 'fakeScan', [barcode]);
};
