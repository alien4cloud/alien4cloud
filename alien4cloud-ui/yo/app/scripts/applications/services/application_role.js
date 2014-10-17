'use strict';

/**
* This service allows to retrieve the available roles for an application from the server.
*/
angular.module('alienUiApp').factory('applicationRolesServices', ['$resource',
  function($resource) {
    return $resource('rest/auth/roles/application', {}, { method: 'GET'}).get().$promise;
  }]);
