define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators').factory('locationResourcesSecurityService', ['$resource', '$alresource', '$http',
    function ($resource, $alresource, $http) {

      var users = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/:resourceId/security/users/:username');

      var groups = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/:resourceId/security/groups/:groupId');

      var applications = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/:resourceId/security/applications/:applicationId');

      var environmentsPerApplication = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/:resourceId/security/environmentsPerApplication');

      var grantUsersBatch = $resource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/security/users', {}, {
        grant: {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var grantGroupsBatch = $resource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/security/groups', {}, {
        grant: {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var revoke = function(type, params, request) {
          return $http({
              url: 'rest/latest/orchestrators/' + params.orchestratorId + '/locations/' + params.locationId + '/resources/security/' + type,
              method: 'DELETE',
              data: {
                  'resources': request.resources,
                  'subjects': request.subjects
              },
              headers: {
                  'Content-Type': 'application/json; charset=UTF-8'
              }
          });
      };

      var revokeUsersBatch = function(params, request) { return revoke('users', params, request);};
      var revokeGroupsBatch = function(params, request) { return revoke('groups', params, request);};

      return {
        'users': users,
        'groups': groups,
        'applications': applications,
        'environmentsPerApplication': environmentsPerApplication,
        'grantUsersBatch': grantUsersBatch,
        'revokeUsersBatch': revokeUsersBatch,
        'grantGroupsBatch': grantGroupsBatch,
        'revokeGroupsBatch': revokeGroupsBatch,
      };
    }]);
});
