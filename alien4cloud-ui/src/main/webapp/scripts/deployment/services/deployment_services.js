define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-deployment').factory('deploymentServices', ['$resource',
    function($resource) {
      var deploymentEventResource = $resource('rest/v1/deployments/:applicationEnvironmentId/events', {}, {
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

      var deploymentsResource = $resource('rest/v1/deployments', {}, {
        'get': {
          method: 'GET',
          params: {
            cloudId: '@orchestratorId',
            applicationId: '@applicationId',
            includeAppSummary: '@includeSourceSummary',
            from: '@from',
            size: '@size'
          },
          isArray: false
        }
      });

      var deploymentStatusResource = $resource('rest/v1/deployments/:deploymentId/status');

      var undeploymentResource = $resource('rest/v1/deployments/:deploymentId/undeploy');

      /*runtime controller*/
      var runtimeTopologyResource = $resource('rest/v1/runtime/:applicationId/environment/:applicationEnvironmentId/topology', {}, {
        'get': {
          method: 'GET',
          params: {
            applicationId: '@applicationId',
            cloudId: '@applicationEnvironmentId'
          },
          isArray: false
        }
      });

      var runtimeResource = $resource('rest/v1/runtime/:applicationId/operations', {}, {
        'executeOperation': {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var nodeInstanceMaintenanceResource = $resource('rest/v1/applications/:applicationId/environments/:applicationEnvironmentId/deployment/:nodeTemplateId/:instanceId/maintenance');

      var deploymentMaintenanceResource = $resource('rest/v1/applications/:applicationId/environments/:applicationEnvironmentId/deployment/maintenance');

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
