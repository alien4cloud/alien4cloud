define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators').factory('orchestratorSecurityService', ['$resource',
    function($resource) {
      /* Users/groups roles on an clouds */
      var manageOrchestratorUserRoles = $resource('rest/latest/orchestrators/:id/roles/users/:username/:role', {}, {
        'addUserRole': {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          },
          params: {
            id: '@id',
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
            id: '@id',
            username: '@username',
            role: '@role'
          }
        }
      });

      var manageOrchestratorGroupRoles = $resource('rest/latest/orchestrators/:id/roles/groups/:groupId/:role', {}, {
        'addGroupRole': {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          },
          params: {
            id: '@id',
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
            id: '@id',
            groupId: '@groupId',
            role: '@role'
          }
        }
      });

      return {
        'userRoles': manageOrchestratorUserRoles,
        'groupRoles': manageOrchestratorGroupRoles
      };
    }]
  );
});
