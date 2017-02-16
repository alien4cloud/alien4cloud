define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators').factory('resourceSecurityFactory', ['$alresource',
    function ($alresource) {

/* resourceUrl MUST NOT ends with a '/' */
      var create = function(resourceUrl, params){

        var users = $alresource(resourceUrl+'/security/users/:username', null, params);

        var groups = $alresource(resourceUrl+'/security/groups/:groupId', null, params);

        var applications = $alresource(resourceUrl+'/security/applications/:applicationId', null, params);

        var environmentsPerApplication = $alresource(resourceUrl+'/security/environmentsPerApplication', null, params);

        return {
          'users': users,
          'groups': groups,
          'applications': applications,
          'environmentsPerApplication': environmentsPerApplication
        };
      };

      return create;

    }]);
});
