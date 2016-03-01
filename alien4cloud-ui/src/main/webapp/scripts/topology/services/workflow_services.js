// define the rest api elements to work with workflow edition.
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-editor', ['ngResource']).factory('workflowServices', ['$resource',
    function($resource) {

      var workflowsResource = $resource('rest/latest/topologies/:topologyId/workflows');
      var workflowResource = $resource('rest/latest/topologies/:topologyId/workflows/:workflowName');
      var workflowInitResource = $resource('rest/latest/topologies/:topologyId/workflows/:workflowName/init');
      var activitiesResource = $resource('rest/latest/topologies/:topologyId/workflows/:workflowName/activities');
      var edgeResource = $resource('rest/latest/topologies/:topologyId/workflows/:workflowName/edges/:from/:to');
      var stepResource = $resource('rest/latest/topologies/:topologyId/workflows/:workflowName/steps/:stepId');
      var fromResource = $resource('rest/latest/topologies/:topologyId/workflows/:workflowName/steps/:stepId/connectFrom');
      var toResource = $resource('rest/latest/topologies/:topologyId/workflows/:workflowName/steps/:stepId/connectTo');
      var swapResource = $resource('rest/latest/topologies/:topologyId/workflows/:workflowName/steps/:stepId/swap');
      
      return {
        'workflows': {
          'create': workflowsResource.save,
          'remove': workflowResource.remove,
          'rename': workflowResource.save,
          'init': workflowInitResource.save
        },
        'edge': {
          'remove': edgeResource.remove
        },
        'step': {
          'remove': stepResource.remove,
          'rename': stepResource.save,
          'connectFrom': fromResource.save,
          'connectTo': toResource.save,
          'swap': swapResource.save
        },
        'activity': {
          'add': activitiesResource.save
        }
      };
    }
  ]);
}); // define
