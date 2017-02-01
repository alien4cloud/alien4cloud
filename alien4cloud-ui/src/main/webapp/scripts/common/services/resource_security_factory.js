define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators').factory('resourceSecurityFactory', ['$alresource',
    function ($alresource) {

/* url MUST NOT ends with a '/' */
      var create = function(url, params){

        var users = $alresource(url+'/security/users/:userId', null, params);

        var groups = $alresource(url+'/security/groups/:groupId', null, params);

        var applications = $alresource(url+'/security/applications/:applicationId', null, params);

        var environmentsPerApplication = $alresource(url+'/security/environmentsPerApplication', null, params);

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
