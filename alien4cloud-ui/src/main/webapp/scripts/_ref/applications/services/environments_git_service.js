define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications').factory('environmentsGitService', ['$resource',
    function($resource) {
      var gitDeploymentConfigResources = $resource('rest/latest/git/deployment/environment/:environmentId', {}, {
        'getByEnvId': {
          method: 'GET',
          params: {
            environmentId: '@environmentId'
          },
          isArray: false
        }
      });

      var gitDeploymentConfigCustomResources = $resource('rest/latest/git/deployment/custom', {}, {
        'update': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var gitDeploymentConfigManagedResources = $resource('rest/latest/git/deployment/managed/:environmentId', {}, {
        'update': {
          method: 'POST',
          isArray: false,
          params: {
            environmentId: '@environmentId'
          },
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      return {
        'getDeploymentConfigGitByEnvId': gitDeploymentConfigResources.getByEnvId,
        'updateDeploymentConfigGitToCustom' : gitDeploymentConfigCustomResources.update,
        'updateDeploymentConfigGitToAlienManaged' : gitDeploymentConfigManagedResources.update
      };
    }
  ]);
});
