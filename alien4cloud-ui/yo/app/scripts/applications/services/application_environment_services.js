'use strict';

angular.module('alienUiApp').factory('applicationEnvironmentServices', ['$resource',
  function($resource) {

    // Search for application environments
    var searchEnvironmentResource = $resource('rest/applications/:applicationId/environments/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var applicationEnvironmentResource = $resource('rest/applications/:applicationId/environments', {}, {
      'create': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var applicationEnvironmentMiscResource = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId', {}, {
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

    var envEnumTypes = $resource('rest/enums/environmenttype', {}, {
      'get': {
        method: 'GET',
        cache : true
      }
    });

    return {
      'get': null,
      'create': applicationEnvironmentResource.create,
      'delete': applicationEnvironmentMiscResource.delete,
      'update': null,
      'environmentTypeList': envEnumTypes.get,
      'searchEnvironment': searchEnvironmentResource.search
    };

  }
]);
