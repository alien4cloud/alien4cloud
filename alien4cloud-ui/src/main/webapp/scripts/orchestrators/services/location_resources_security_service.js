define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators').factory('locationResourcesSecurityService', ['$alresource',
    function ($alresource) {

      var users = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/:resourceId/security/users/:username');

      var groups = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/:resourceId/security/groups/:groupId');

      var applications = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:locationId/resources/:resourceId/security/applications/:applicationId');

      return {
        'users': users,
        'groups': groups,
        'applications': applications
      };
    }]);
});
