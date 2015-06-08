'use strict';

angular.module('alienUiApp').factory('applicationVersionServices', ['$resource',
  function($resource) {
    var searchVersionResource = $resource('rest/applications/:delegateId/versions/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var applicationVersionResource = $resource('rest/applications/:delegateId/versions', {}, {
      'create': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'get': {
        method: 'GET'
      }
    });

    var applicationVersionMiscResource = $resource('rest/applications/:delegateId/versions/:versionId', {}, {
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
      'getFirst': applicationVersionResource.get,
      'create': applicationVersionResource.create,
      'get': applicationVersionMiscResource.get,
      'delete': applicationVersionMiscResource.delete,
      'update': applicationVersionMiscResource.update,
      'searchVersion': searchVersionResource.search
    };

  }
]);
