// ElasticSearch client utilities

'use strict';

var http = require('./simplehttp');

function getOptions(method, indexName, typeName, content) {
  var path = '/' + indexName;
  if(typeName && typeName!==null) {
    path = path + '/' + typeName;
  }
  var options = {
    host: 'localhost',
    port: '9200',
    path: path,
    method: method,
    headers: {
      'Content-Type': 'application/json; charset=UTF-8',
      // 'Content-Length': content.length
    }
  };
  if(content && content !== null) {
    options.headers['Content-Length'] = content.length;
  }
  return options;
}

function buildOptions(method, indexName, query) {
  var options = getOptions(method, indexName, null, query);
  options.path = options.path + '/_query';
  return options;
}

module.exports.delete = function(indexName, query) {
  if (query === undefined) {
    query = JSON.stringify({
      'query': {
        'match_all': {}
      }
    });
  }
  var options = buildOptions('DELETE', indexName, query);
  return http.call(options, query);
};

module.exports.index = function(indexName, typeName, content) {
  var options = getOptions('POST', indexName, typeName);
  return http.call(options, content);
};
