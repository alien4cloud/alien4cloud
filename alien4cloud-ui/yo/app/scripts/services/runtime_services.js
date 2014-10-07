'use strict';

angular.module('alienUiApp').factory('runtimeService', ['$resource',
  function($resource) {
    var deploymentEventResource = $resource('rest/deployments/:id/events', {}, {
      'get': {
        method: 'GET',
        params: {
          id: '@id',
          from: '@from',
          size: '@size'
        },
        isArray: false
      }
    });

    return {
      'getEvents': deploymentEventResource.get
    };
  }]);
