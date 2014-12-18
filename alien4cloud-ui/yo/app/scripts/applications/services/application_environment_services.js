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
        cache: true
      }
    });

    /*Users roles on an environment*/
    var manageEnvUserRoles = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId/userRoles/:username/:role', {}, {
      'addUserRole': {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        },
        params: {
          applicationEnvironmentId: '@applicationEnvironmentId',
          applicationId: '@applicationId',
          username: '@username',
          role: '@role'
        }
      },
      'removeUserRole': {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        },
        params: {
          applicationEnvironmentId: '@applicationEnvironmentId',
          applicationId: '@applicationId',
          username: '@username',
          role: '@role'
        }
      }
    });

    var manageEnvGroupRoles = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId/groupRoles/:groupId/:role', {}, {
      'addGroupRole': {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        },
        params: {
          applicationEnvironmentId: '@applicationEnvironmentId',
          applicationId: '@applicationId',
          groupId: '@groupId',
          role: '@role'
        }
      },
      'removeGroupRole': {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        },
        params: {
          applicationEnvironmentId: '@applicationEnvironmentId',
          applicationId: '@applicationId',
          groupId: '@groupId',
          role: '@role'
        }
      }
    });

    return {
      'get': null,
      'create': applicationEnvironmentResource.create,
      'delete': applicationEnvironmentMiscResource.delete,
      'update': null,
      'environmentTypeList': envEnumTypes.get,
      'searchEnvironment': searchEnvironmentResource.search,
      'userRoles': manageEnvUserRoles,
      'groupRoles': manageEnvGroupRoles
    };

  }
]);
