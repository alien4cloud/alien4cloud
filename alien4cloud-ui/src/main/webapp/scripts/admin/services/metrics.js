'use strict';

angular.module('alienUiApp').factory('metricsService', ['$resource', function ($resource) {
  return $resource('rest/admin/metrics/metrics', {}, {
    'get': { method: 'GET'}
  });
}]);

angular.module('alienUiApp').factory('threadDumpService', ['$http', function ($http) {
  return {
    dump: function() {
      var promise = $http.get('rest/admin/dump').then(function(response){
        return response.data;
      });
      return promise;
    }
  };
}]);

angular.module('alienUiApp').factory('healthCheckService', ['$http', function ($http) {
  return {
    check: function() {
      var promise = $http.get('rest/admin/health').then(function(response){
        return response.data;
      });
      return promise;
    }
  };
}]);
