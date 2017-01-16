define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators').factory('locationSecurityService', ['$resource',
    function($resource) {

      var getLocationUsers = $resource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/security/users', {}, {
        'get': {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          },
          params: {
            orchestratorId: '@orchestratorId',
            locationId: '@locationId'
          }
        },
      });

      var manageLocationUserRoles = $resource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/security/users/:username', {}, {
        'authorize': {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          },
          params: {
            orchestratorId: '@orchestratorId',
            locationId: '@locationId',
            username: '@username'
          }
        },
        'rewoke': {
          method: 'DELETE',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          },
          params: {
            orchestratorId: '@orchestratorId',
            locationId: '@locationId',
            username: '@username'
          }
        }
      });

      var manageLocationGroupRoles = $resource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/roles/groups/:groupId/:role', {}, {
        'addGroupRole': {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          },
          params: {
            orchestratorId: '@orchestratorId',
            locationId: '@locationId',
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
            orchestratorId: '@orchestratorId',
            locationId: '@locationId',
            groupId: '@groupId',
            role: '@role'
          }
        }
      });

      return {
        'getLocationUsers': getLocationUsers.get,
        'userRoles': manageLocationUserRoles,
        'groupRoles': manageLocationGroupRoles
      };
    }]);
});
