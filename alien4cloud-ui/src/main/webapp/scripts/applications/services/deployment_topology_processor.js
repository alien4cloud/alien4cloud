define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/list_to_map_service');
  require('scripts/topology/services/topology_json_processor');
  require('scripts/orchestrators/services/location_resources_processor');

  modules.get('a4c-orchestrators').factory('deploymentTopologyProcessor',
    ['listToMapService', 'topologyJsonProcessor', 'locationResourcesProcessor',
      function(listToMapService, topologyJsonProcessor, locationResourcesProcessor) {
        // This service post-process a deployment topology in order to add maps field from ordered maps array (array of MapEntry).
        return {
          process: function(deploymentTopology) {
            topologyJsonProcessor.process(deploymentTopology);
            if (!_.isEmpty(deploymentTopology.topology.substitutedNodes)) {
              locationResourcesProcessor.processLocationResourceTemplatesMap(deploymentTopology.topology.substitutedNodes);
            }
          },
          processSubstitutionResources: function(substitutionResources) {
            var availableSubstitutionsIds = substitutionResources.availableSubstitutions;
            substitutionResources.availableSubstitutions = {};
            for (var nodeId in availableSubstitutionsIds) {
              if (availableSubstitutionsIds.hasOwnProperty(nodeId)) {
                substitutionResources.availableSubstitutions[nodeId] = _.map(availableSubstitutionsIds[nodeId], function(resourceId) {
                  return substitutionResources.substitutionsTemplates[resourceId];
                });
              }
            }
            listToMapService.processMap(substitutionResources.substitutionTypes.nodeTypes, 'properties');
            listToMapService.processMap(substitutionResources.substitutionTypes.capabilityTypes, 'properties');
            for (var nodeId in substitutionResources.availableSubstitutions) {
              if (substitutionResources.availableSubstitutions.hasOwnProperty(nodeId)) {
                locationResourcesProcessor.processLocationResourceTemplates(substitutionResources.availableSubstitutions[nodeId]);
              }
            }
          }
        };
      } // function
    ]); // factory
}); // define
