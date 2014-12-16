'use strict';

angular.module('alienUiApp').factory('applicationVersionServices', ['$resource',
  function($resource) {

    var versionFormDescriptor = function() {
      return {
        "_type": "complex",
        "_order": ["version", "description"],
        "_propertyType": {
          "version": {
            "_label": "APPLICATIONS.VERSION.VERSION",
            "_type": "string",
            "_notNull": true,
            "_constraints": [
              {
                "pattern": "\\d+(?:\\.\\d+)*(?:[\\.-]\\p{Alnum}+)*"
              }
            ]
          },
          "description": {
            "_label": "APPLICATIONS.VERSION.DESCRIPTION",
            "_type": "string",
            "_notNull": false
          }
        }
      };
    };

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

    var applicationVersionMiscResource = $resource('rest/applications/:applicationId/versions/:applicationVersionId', {}, {
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
      'get': applicationVersionMiscResource.get,
      'create': applicationVersionResource.create,
      'delete': applicationVersionMiscResource.delete,
      'update': null,
      'searchVersion': searchVersionResource.search,
      'getFormDescriptor': versionFormDescriptor
    };

  }
]);
