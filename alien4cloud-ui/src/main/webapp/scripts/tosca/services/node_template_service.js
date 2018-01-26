define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('scripts/tosca/services/tosca_service');

  modules.get('a4c-tosca').factory('nodeTemplateService', ['toscaService',
    function(toscaService) {
      return {
        /**
         * Get a container requirement for a given node template.
         *
         * @param nodeTemplate The node template from which to get the container requirement.
         * @param nodeTypes A map of node types that must contains the node template type.
         * @param relationshipTypes A map of relationship types that may contains the types of relationships that may be directly defined by requirements.
         * @param capabilityTypes A map of capability types that must contains the types of the capabilites defined by the node template's requirements.
         * @return The first container requirement found on the node template or null if none has been found.
         */
        getContainerRequirement: function(nodeType, relationshipTypes, capabilityTypes) {
          for (var i = 0; i < nodeType.requirements.length; i++) {
            var requirementDefinition = nodeType.requirements[i];
            if (toscaService.isContainerType(requirementDefinition.type, capabilityTypes)) {
              return requirementDefinition.id;
            }
            // the requirement is not a Container capability requirement but if it may define a target node and still a HostedOn relationship
            if (_.defined(requirementDefinition.relationshipType)) {
              // TODO make sure that we get the relationship type if not already in the relationshipTypes map.
              if (toscaService.isHostedOnType(requirementDefinition.relationshipType, relationshipTypes)) {
                return requirementDefinition.id;
              }
            }
          }
          // no container requirement has been found, return null.
          return null;
        },

        getNodeTypeIcon: function(nodeType) {
          if (_.isNotEmpty(nodeType) && _.isNotEmpty(nodeType.tags)) {
            var icons = _.find(nodeType.tags, {'name': 'icon'});
            if (_.isNotEmpty(icons)) {
              return icons.value;
            }
          }
        }
      };
    } // function
  ]); // factory
}); // define
