/* global protractor */

'use strict';

var http = require('http');
var settings = require('./settings');

module.exports.call = function(options, content, forcelog) {
  var defer = protractor.promise.defer();
  // Set up the request
  var request = http.request(options, function(response) {
    defer.fulfill(response);
    response.setEncoding('utf8');
    response.on('data', function(chunk) {
      if (forcelog || settings.debug) {
        console.log('ES request method [' + options.method + '] path [' + options.path + '] content [' + content + '] received [' + chunk + ']');
      }
    });
  });
  request.on('error', function(e) {
    console.log('################# call on path [' + options.path + '] request encountered error [' + e.message + ']');
    defer.reject({
      error: e,
      message: '################# call on path [' + options.path + '] request encountered error [' + e.message + ']'
    });
  });

  if(content && content !== null) {
    request.write(content);
  }
  request.end();
  return defer.promise;
};
