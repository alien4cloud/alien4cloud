'use strict';

angular.module('alienAuth').factory('alienUserService', ['$resource', function($resource) {
  var crudUsers = $resource('rest/users/:username', {}, {
    'get': {
      method: 'GET',
      isArray: false,
      params: {
        username: '@username'
      }
    }
  });

  return crudUsers;
}]);
