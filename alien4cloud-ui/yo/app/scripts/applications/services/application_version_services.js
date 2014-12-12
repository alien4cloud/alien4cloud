'use strict';

angular.module('alienUiApp').factory('applicationVersionServices', ['$resource',
  function($resource) {

    var searchVersionResource = $resource('rest/applications/:applicationId/versions/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var applicationVersionResource = $resource('rest/applications/:applicationId/versions', {}, {
      'create': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var applicationVersionMiscResource = $resource('rest/applications/:applicationId/versions/:applicationEnvironmentId', {}, {
      'get': {
        method: 'GET'
      },
      'delete': {
        method: 'DELETE'
      },
      'update': {
        method: 'PUT'
      }
    });

    return {
      'get': null,
      'create': applicationVersionResource.create,
      'delete': applicationVersionMiscResource.delete,
      'update': null,
      'searchVersion': searchVersionResource.search
    };

  }
]);
