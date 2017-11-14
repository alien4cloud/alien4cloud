define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/list_to_map_service');
  require('scripts/topology/services/topology_json_processor');
  require('scripts/orchestrators/services/location_resources_processor');

  modules.get('a4c-applications').factory('deploymentTopologyProcessor',
    ['listToMapService', 'topologyJsonProcessor', 'locationResourcesProcessor',
      function(listToMapService, topologyJsonProcessor, locationResourcesProcessor) {
        // This service post-process a deployment topology in order to add maps field from ordered maps array (array of MapEntry).
        return {
          process: function(deploymentTopology) {
            topologyJsonProcessor.process(deploymentTopology);
            if (!_.isEmpty(deploymentTopology.topology.substitutedNodes) && _.defined(deploymentTopology.availableSubstitutions.substitutionsTemplates)) {
              for (var nodeId in deploymentTopology.topology.substitutedNodes) {
                if (deploymentTopology.topology.substitutedNodes.hasOwnProperty(nodeId)) {
                  var locationResourceTemplateId = deploymentTopology.topology.substitutedNodes[nodeId];
                  deploymentTopology.topology.substitutedNodes[nodeId] = _.cloneDeep(deploymentTopology.availableSubstitutions.substitutionsTemplates[locationResourceTemplateId]);
                  deploymentTopology.topology.substitutedNodes[nodeId].template = deploymentTopology.topology.matchReplacedNodes[nodeId];
                }
              }
            }

            // policies
            if (!_.isEmpty(deploymentTopology.topology.substitutedPolicies) && _.defined(deploymentTopology.availableSubstitutions.substitutionsPoliciesTemplates)) {
              _.forEach(deploymentTopology.topology.substitutedPolicies, function(templateId, policyId){
                deploymentTopology.topology.substitutedPolicies[policyId] = _.cloneDeep(deploymentTopology.availableSubstitutions.substitutionsPoliciesTemplates[templateId]);
                deploymentTopology.topology.substitutedPolicies[policyId].template = deploymentTopology.topology.policies[policyId];
              });
            }

            if (_.defined(deploymentTopology.availableSubstitutions)) {
              this.processSubstitutionResources(deploymentTopology.availableSubstitutions);
            }
          },
          processSubstitutionResources: function(substitutionResources) {
            var availableSubstitutionsIds = substitutionResources.availableSubstitutions;
            substitutionResources.availableSubstitutions = {};
            function mapTemplates(resourceId) {
              return substitutionResources.substitutionsTemplates[resourceId];
            }
            for (var nodeTemplateId in availableSubstitutionsIds) {
              if (availableSubstitutionsIds.hasOwnProperty(nodeTemplateId)) {
                substitutionResources.availableSubstitutions[nodeTemplateId] = _.map(availableSubstitutionsIds[nodeTemplateId], mapTemplates);
              }
            }
            listToMapService.processMap(substitutionResources.substitutionTypes.nodeTypes, 'properties');
            listToMapService.processMap(substitutionResources.substitutionTypes.capabilityTypes, 'properties');
            _.forEach(substitutionResources.availableSubstitutions, function(templates){
              locationResourcesProcessor.processLocationResourceTemplates(templates);
            });

            //policies
            var availablePoliciesSubstitutionsIds = substitutionResources.availablePoliciesSubstitutions;
            substitutionResources.availablePoliciesSubstitutions = {};
            _.forEach(availablePoliciesSubstitutionsIds, function(value, policyId){
              substitutionResources.availablePoliciesSubstitutions[policyId] = _.map(value, function(resourceId){
                return substitutionResources.substitutionsPoliciesTemplates[resourceId];
              });
            });
            listToMapService.processMap(substitutionResources.substitutionTypes.policyTypes, 'properties');
            _.forEach(substitutionResources.availablePoliciesSubstitutions, function(templates){
              locationResourcesProcessor.processTemplates(templates);
            });
          }
        };
      } // function
    ]); // factory
}); // define
