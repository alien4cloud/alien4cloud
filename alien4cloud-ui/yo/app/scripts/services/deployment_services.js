'use strict';

angular.module('alienUiApp').factory('deploymentServices', ['$resource',
  function($resource) {
    var deploymentEventResource = $resource('rest/deployments/:topologyId/events', {}, {
      'get': {
        method: 'GET',
        params: {
          topologyId: '@topologyId',
          cloudId: '@cloudId',
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

    return {
      'get': deploymentsResource.get,
      'getEvents': deploymentEventResource.get,
      'getStatus': deploymentStatusResource.get,
      'undeploy': undeploymentResource.get,
      'runtime': {
        'getTopology': runtimeTopologyResource.get,
        'executeOperation': runtimeResource.executeOperation
      }
    };
  }]);
