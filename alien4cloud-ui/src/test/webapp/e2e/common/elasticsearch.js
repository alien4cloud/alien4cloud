// ElasticSearch client utilities
/* global protractor */

'use strict';

var settings = require('./settings');
var http = require('http');

function getOptions(method, indexName, content, typeName) {
  var path = '/' + indexName;
  if(typeName && typeName!==null) {
    path = path + '/' + typeName;
  }
  return {
    host: 'localhost',
    port: '9200',
    path: path,
    method: method,
    headers: {
      'Content-Type': 'application/json; charset=UTF-8',
      'Content-Length': content.length
    }
  };
}

function buildOptions(method, indexName, query) {
  var options = getOptions(method, indexName, query);
  options.path = options.path + '/_query';
  return options;
}

function call(method, options, indexName, content) {
  var defer = protractor.promise.defer();
  // Set up the request
  var request = http.request(options, function(res) {
    res.setEncoding('utf8');
    res.on('data', function(chunk) {
      if (settings.debug) {
        console.log('ES request method [' + method + '] index [' + indexName + '] content [' + content + '] received [' + chunk + ']');
      }
      defer.fulfill(res);
    });
  });
  request.on('error', function(e) {
    console.log('################# Clean index [' + indexName + '] request encountered error [' + e.message + ']');
    defer.reject({
      error: e,
      message: '################# Clean index [' + indexName + '] request encountered error [' + e.message + ']'
    });
  });

  request.write(content);
  request.end();
  return defer.promise;
}

module.exports.delete = function(indexName, query) {
  if (query === undefined) {
    query = JSON.stringify({
      'query': {
        'match_all': {}
      }
    });
  }
  var method = 'DELETE';
  var options = buildOptions(method, indexName, query);
  return call(method, options, indexName, query);
};

module.exports.index = function(indexName, typeName, content) {
  var method = 'POST';
  var options = getOptions(method, indexName, content, typeName);
  return call(method, options, indexName, content);
};
