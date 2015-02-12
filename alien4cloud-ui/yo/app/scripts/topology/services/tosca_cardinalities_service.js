/* global UTILS */
'use strict';

angular.module('alienUiApp').factory('toscaCardinalitiesService',
  function() {
    // This service updates capabilities and requirements cardinalities for a topology based on current relationships.
    return {
      /** Static variable for unbounded. */
      UNBOUNDED: 'unbounded',

      /**
      * Compute the number of times that a requirement template is used by relationships.
      *
      * @param requirement The requirement for which to compute usage.
      * @param relationships The relationships that have as source the node template that contains the given requirement.
      * @return the number of times the given requirement is used for it's node template.
      */
      computeRequirementUsage: function() {
        if(UTILS.isUndefinedOrNull(this.relationships)) {
          return 0;
        }
        var count = 0;
        for(var i=0; i< this.relationships.length; i++) {
          var relationship = this.relationships[i];
          if(relationship.requirementName === this.requirement.key && relationship.requirementType === this.requirement.type) {
            count++;
          }
        }
        return count;
      },

      /**
      * Compute the number of times that a capability template is used by relationships.
      *
      * @param nodeTemplate The node template that holds the capability.
      * @param capability The capability for which to compute usage.
      * @param nodeTemplates The map of all node templates in the topology.
      * @return the number of times the given capability is used in the topology.
      */
      computeCapabilityUsage: function() {
        var count = 0;
        for (var templateName in this.nodeTemplates) {
          var nodeTemp = this.nodeTemplates[templateName];
          var relationships = nodeTemp.relationships;
          if (UTILS.isUndefinedOrNull(relationships)) {
            continue;
          }
          for (var relName in relationships) {
            var relTemplate = relationships[relName];
            if (UTILS.isDefinedAndNotNull(relTemplate.target) && relTemplate.target === this.nodeTemplate.name && relTemplate.requirementType === this.capability.type) {
              count++;
            }
          }
        }
        return count;
      },

      /**
      * Compute the remaining connections possible for the bound of a capability or requirement.
      *
      * @param element the Bounded element (requirement or capability).
      * @param usageCallable Callable object (has a call method) that computes the count usage of the given element.
      * @return object that contains a 'yes' flag set to true if the upper bound is not reached or false if the upper bound is reached and a 'remaining' field that contains the number of element that we can still add to the bounded element.
      */
      computeBoundRemains: function(element, usageCallable) {
        var remains = {
          yes: true,
          remaining: element.upperBound
        };

        if(remains.remaining === this.UNBOUNDED) {
          return remains;
        }

        // if the bound is majored compute the remanining.
        var count = usageCallable.call();

        remains.yes = count < remains.remaining;
        remains.remaining = remains.yes ? remains.remaining - count : 0;
        return remains;
      },

      /**
      * Enrich requirements of a given node template with bound informations computed from the topology.
      *
      * @param nodeTypes Map of node types used in the topology.
      * @param nodeTemplate Node template for which to enrich requirement bounds.
      */
      fillRequirementBounds: function(nodeTypes, nodeTemplate) {
        var instance = this;
        var nodeType = nodeTypes[nodeTemplate.type];
        nodeType.requirements.forEach(function(reqDef) {
          var requirement = nodeTemplate.requirementsMap[reqDef.id].value;
          requirement.upperBound = reqDef.upperBound;
          requirement.lowerBound = reqDef.lowerBound;
          requirement.canAddRel = instance.computeBoundRemains(requirement, {
            requirement: requirement,
            relationships: nodeTemplate.relationships,
            call: instance.computeRequirementUsage
          });
        });
      },

      /**
      * Enrich capabilities of a given node template with bound informations computed from the topology.
      *
      * @param nodeTypes Map of node types used in the topology.
      * @param nodeTemplates Map of node templates defined in the topology.
      * @param nodeTemplate Node template for which to enrich requirement bounds.
      */
      fillCapabilityBounds: function(nodeTypes, nodeTemplates, nodeTemplate) {
        var instance = this;
        var nodeType = nodeTypes[nodeTemplate.type];
        if (nodeType.capabilities) {
          nodeType.capabilities.forEach(function(capaDef) {
            var capability = nodeTemplate.capabilitiesMap[capaDef.id].value;
            capability.upperBound = capaDef.upperBound;
            capability.canAddRel = instance.computeBoundRemains(capability, {
              capability: capability,
              nodeTemplate: nodeTemplate,
              nodeTemplates: nodeTemplates,
              call: instance.computeCapabilityUsage
            });
          });
        }
      }
    };
  } // function
);
