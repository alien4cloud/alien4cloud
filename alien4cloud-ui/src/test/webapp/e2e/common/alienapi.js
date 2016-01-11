'use strict';

var http = require('./simplehttp');

function getOptions(method, path, cookies) {
  var options = {
    host: 'localhost',
    port: '8088',
    path: path,
    method: method,
    headers: {
      'Content-Type': 'application/json; charset=UTF-8'
    }
  };
  if (cookies && cookies !== null) {
    options.headers['Cookie'] = cookies.join("; ");
  }
  return options;
}

module.exports.login = function(username, password) {
  var options = getOptions('POST', '/login');
  var data = 'username=' + username + '&password=' + password + '&submit=Login';
  options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
  return http.call(options, data);
};

module.exports.enablePlugin = function(pluginId, cookies) {
  var options = getOptions('GET', '/rest/plugins/' + pluginId + '/enable', cookies);
  return http.call(options, null);
};

module.exports.enableOrchestrator = function(orchestratorId, cookies) {
  var options = getOptions('POST', '/rest/orchestrators/' + orchestratorId + '/instance', cookies);
  return http.call(options, null);
};

module.exports.initPlatform = function(cookies) {
  var options = getOptions('POST', '/rest/maintenance/init-platform', cookies);
  return http.call(options, null);
};

module.exports.teardownPlatform = function(cookies) {
  var options = getOptions('POST', '/rest/maintenance/teardown-platform', cookies);
  return http.call(options, null);
};
