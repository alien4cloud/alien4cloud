define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators').factory('locationResourcesSecurityService', ['$resource', '$alresource',
    function ($resource, $alresource) {

      var users = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/:resourceId/security/users/:username');

      var groups = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/:resourceId/security/groups/:groupId');

      var applications = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/:resourceId/security/applications/:applicationId');

      var environmentsPerApplication = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/:resourceId/security/environmentsPerApplication');

      var usersBatch = $resource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/security/users', {}, {
        grant: {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        },
        revoke: {
          method: 'DELETE',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        },
      });

      var groupsBatch = $resource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/security/groups', {}, {
        grant: {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        },
        revoke: {
          method: 'DELETE',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        },
      });

      return {
        'users': users,
        'groups': groups,
        'applications': applications,
        'environmentsPerApplication': environmentsPerApplication,
        'usersBatch': usersBatch,
        'groupsBatch': groupsBatch
      };
    }]);
});
