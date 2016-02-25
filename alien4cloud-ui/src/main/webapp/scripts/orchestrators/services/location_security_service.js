define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators').factory('locationSecurityService', ['$resource',
    function($resource) {
      /* Users/groups roles on an clouds */
      var manageLocationUserRoles = $resource('rest/orchestrators/:orchestratorId/locations/:locationId/roles/users/:username/:role', {}, {
        'addUserRole': {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          },
          params: {
            orchestratorId: '@orchestratorId',
            locationId: '@locationId',
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
            orchestratorId: '@orchestratorId',
            locationId: '@locationId',
            username: '@username',
            role: '@role'
          }
        }
      });

      var manageLocationGroupRoles = $resource('rest/orchestrators/:orchestratorId/locations/:locationId/roles/groups/:groupId/:role', {}, {
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
        'userRoles': manageLocationUserRoles,
        'groupRoles': manageLocationGroupRoles
      };
    }]);
});
