define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-deployment').factory('deploymentServices', ['$resource',
    function($resource) {
      var deploymentEventResource = $resource('rest/latest/deployments/:applicationEnvironmentId/events', {}, {
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

      var deploymentsResource = $resource('rest/latest/deployments/:deploymentId', {}, {
        'search': {
          method: 'GET',
          params: {
            cloudId: '@orchestratorId',
            applicationId: '@applicationId',
            includeAppSummary: '@includeSourceSummary',
            from: '@from',
            size: '@size'
          },
          isArray: false
        },
        'get': {
          method: 'GET',
          isArray: false
        }
      });

      var deploymentStatusResource = $resource('rest/latest/deployments/:deploymentId/status');

      var undeploymentResource = $resource('rest/latest/deployments/:deploymentId/undeploy');

      var lastWorkflowResumeResource = $resource('rest/latest/deployments/:deploymentId/resume-last-execution', {}, {
        'resume': {
          method: 'POST',
          params: {
            deploymentId : '@deploymentId'
          }
        }
      });

      /*runtime controller*/
      var runtimeTopologyResource = $resource('rest/latest/runtime/:applicationId/environment/:applicationEnvironmentId/topology', {}, {
        'get': {
          method: 'GET',
          params: {
            applicationId: '@applicationId',
            cloudId: '@applicationEnvironmentId'
          },
          isArray: false
        }
      });

      var runtimeResource = $resource('rest/latest/runtime/:applicationId/operations', {}, {
        'executeOperation': {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var nodeInstanceMaintenanceResource = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/deployment/:nodeTemplateId/:instanceId/maintenance');

      var deploymentMaintenanceResource = $resource('rest/latest/applications/:applicationId/environments/:applicationEnvironmentId/deployment/maintenance');

      var nonNativeNodesResource = $resource('rest/latest/runtime/:applicationId/environment/:applicationEnvironmentId/nonNatives');

      return {
        'getById': deploymentsResource.get,
        'get': deploymentsResource.search,
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
        },
        'getNonNativeNodes' : nonNativeNodesResource.get,
        'resumeLastWorkflow' : lastWorkflowResumeResource.resume
      };
    }
  ]);
});
