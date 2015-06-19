define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-deployment').factory('deploymentServices', ['$resource',
    function($resource) {
      var deploymentEventResource = $resource('rest/deployments/:applicationEnvironmentId/events', {}, {
        'get': {
          method: 'GET',
          params: {
            applicationEnvironmentId: '@applicationEnvironmentId',
            from: '@from',
            size: '@size'
          },
          isArray: false
        }
      });

      var deploymentsResource = $resource('rest/deployments', {}, {
        'get': {
          method: 'GET',
          params: {
            cloudId: '@cloudId',
            applicationId: '@applicationId',
            includeAppSummary: '@includeAppSummary',
            from: '@from',
            size: '@size'
          },
          isArray: false
        }
      });

      var deploymentStatusResource = $resource('rest/deployments/:deploymentId/status');

      var undeploymentResource = $resource('rest/deployments/:deploymentId/undeploy');

      /*runtime controller*/
      var runtimeTopologyResource = $resource('rest/runtime/:applicationId/environment/:applicationEnvironmentId/topology', {}, {
        'get': {
          method: 'GET',
          params: {
            applicationId: '@applicationId',
            cloudId: '@applicationEnvironmentId'
          },
          isArray: false
        }
      });

      var runtimeResource = $resource('rest/runtime/:applicationId/operations', {}, {
        'executeOperation': {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var nodeInstanceMaintenanceResource = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId/deployment/:nodeTemplateId/:instanceId/maintenance');

      var deploymentMaintenanceResource = $resource('rest/applications/:applicationId/environments/:applicationEnvironmentId/deployment/maintenance');

      return {
        'get': deploymentsResource.get,
        'getEvents': deploymentEventResource.get,
        'getStatus': deploymentStatusResource.get,
        'undeploy': undeploymentResource.get,
        'runtime': {
          'getTopology': runtimeTopologyResource.get,
          'executeOperation': runtimeResource.executeOperation
        },
        'nodeInstanceMaintenanceOn': nodeInstanceMaintenanceResource.save,
        'nodeInstanceMaintenanceOff': nodeInstanceMaintenanceResource.remove,
        'deploymentMaintenance': {
          'on': deploymentMaintenanceResource.save,
          'off': deploymentMaintenanceResource.remove
        }
      };
    }
  ]);
});
