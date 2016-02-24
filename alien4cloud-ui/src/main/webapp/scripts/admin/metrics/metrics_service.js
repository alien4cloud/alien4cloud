define(function (require) {
  'use strict';
  
  var modules = require('modules');
  var admin = modules.get('alien4cloud-admin');

  admin.factory('metricsService', ['$resource', function ($resource) {
    return $resource('rest/v1/admin/metrics/metrics', {}, {
      'get': { method: 'GET'}
    });
  }]);

  admin.factory('threadDumpService', ['$http', function ($http) {
    return {
      dump: function() {
        var promise = $http.get('rest/v1/admin/dump').then(function(response){
          return response.data;
        });
        return promise;
      }
    };
  }]);

  admin.factory('healthCheckService', ['$http', function ($http) {
    return {
      check: function() {
        var promise = $http.get('rest/v1/admin/health').then(function(response){
          return response.data;
        });
        return promise;
      }
    };
  }]);
});
