/* global UTILS */
'use strict';

angular.module('alienUiApp').factory('relationshipTopologyService', ['$q', 'componentService', 'toscaService',
  function($q, componentService, toscaService) {
    return {
      /**
      * Compute a list of node templates that can be valid targets for a given requirement.
      *
      * @param sourceElementName: Name of the node template that is source of the relationship.
      * @param requirement: The actual requirement for which to find targets.
      * @param requirementName: The name of the requirement for which to find targets.
      * @param nodeTemplates: A map of node templates in which to find valid targets for the requirement.
      * @param nodeTypes: A map of node types that should contains all types used by the previous node templates map.
      * @param relationshipTypes: A map of relationship types used in the topology.
      * @param capabilityTypes: A map of relationship types used by nodes in the topology.
      * @param dependencies: Array of CSAR dependencies in which types should exists.
      * @param preferedTargetName: Optional name of a prefered target node template that will be placed in prefered field of the result object.
      */
      getTargets: function(sourceElementName, requirement, requirementName, nodeTemplates, nodeTypes, relationshipTypes, capabilityTypes, dependencies, preferedTargetName) {
        var instance = this;
        // create a promise as the result may be asynchronous...
        var deferred = $q.defer();

        var requirementDefinition = this.getRequirementDefinition(nodeTypes[nodeTemplates[sourceElementName].type], requirementName);

        if(UTILS.isDefinedAndNotNull(requirementDefinition.relationshipType)) {
          var relationshipType = relationshipTypes[requirementDefinition.relationshipType];
          if(UTILS.isDefinedAndNotNull(relationshipType)) {
            deferred.resolve(instance.doGetTargets(sourceElementName, requirement, nodeTemplates, nodeTypes, capabilityTypes, relationshipType, preferedTargetName));
          } else {
            // valid target for the relationship type
            componentService.getInArchives(requirementDefinition.relationshipType, 'RELATIONSHIP_TYPE', dependencies).success(function(result) {
              deferred.resolve(instance.doGetTargets(sourceElementName, requirement, nodeTemplates, nodeTypes, capabilityTypes, result.data, preferedTargetName));
            });
          }
        } else {
          deferred.resolve(instance.doGetTargets(sourceElementName, requirement, nodeTemplates, nodeTypes, capabilityTypes, null, preferedTargetName));
        }
        // return the promise.
        return deferred.promise;
      },

      getRequirementDefinition: function(nodeType, requirementName) {
        for(var i=0; i<nodeType.requirements.length; i++) {
          if(nodeType.requirements[i].id === requirementName) {
            return nodeType.requirements[i];
          }
        }
        return null;
      },

      doGetTargets: function(sourceElementName, requirement, nodeTemplates, nodeTypes, capabilityTypes, relationshipType, preferedTargetName) {
        var matches = [];
        var preferedMatch = null;

        // valid targets is an array of array, first level elements are all required (AND) while only one of the elements of inner array is required.
        var validTargets = [[requirement.type]];
        if(UTILS.isDefinedAndNotNull(relationshipType) && UTILS.isDefinedAndNotNull(relationshipType.validTargets) && relationshipType.validTargets.length > 0) {
          validTargets.push(relationshipType.validTargets);
        }

        for (var templateName in nodeTemplates) {
          if (templateName !== sourceElementName && nodeTemplates.hasOwnProperty(templateName)) {
            var candidate = nodeTemplates[templateName];
            // try to match on node type first (if match this means that there is no capacity constraint, we won't be able to match cardinalities on theses relationships).
            // but that's allowed by TOSCA Simple profile.
            var match =  this.getNodeTarget(validTargets, candidate, nodeTypes);
            if(match === null) {
              match = this.getCapabilityTarget(validTargets, candidate, nodeTypes, capabilityTypes);
            }
            if(match !== null) {
              matches.push(match);
            }
            if(templateName === preferedTargetName) {
              preferedMatch = match;
            }
          }
        }

        return { targets: matches,  relationshipType: relationshipType, preferedMatch: preferedMatch };
      },

      /**
      * Check if the given node type is a valid candidate based on the validTargets requirements.
      *
      * @param validTargets An array of array. First level array contains conditions that must all be matched (AND). Inner array contains type names, one of them must be matched (OR).
      * @param candidateNodeTypeName Name of the type of the node that is candidate.
      * @param nodeTypes A map of all available node types (it must contains the candidate node type).
      */
      getNodeTarget: function(validTargets, candidateTemplate, nodeTypes) {
        var isValid = true;
        for(var i=0; i<validTargets.length; i++) {
          isValid = isValid && toscaService.isOneOfType(validTargets[i], candidateTemplate.type, nodeTypes);
        }
        if(isValid) {
          return { template: candidateTemplate, capabilities: [] };
        }
        return null;
      },

      getCapabilityTarget: function(validTargets, candidateTemplate, nodeTypes, capabilityTypes) {
        var nodeCapabilities = nodeTypes[candidateTemplate.type].capabilities;
        if(UTILS.isUndefinedOrNull(nodeCapabilities)) {
          return null;
        }

        var match = null;
        for(var i=0; i<nodeCapabilities.length; i++) {
          var capabilityId = nodeCapabilities[i].id;
          if(candidateTemplate.capabilities[capabilityId].canAddRel.yes &&
            this.isValidTarget(validTargets, candidateTemplate.type, i, nodeTypes, capabilityTypes)) {
            if(match === null) {
              match = { template: candidateTemplate, capabilities: [] };
            }
            match.capabilities.push({id: capabilityId, type: candidateTemplate.capabilities[capabilityId].type});
          }
        }
        return match;
      },

      /**
      * Check if a capability is a valid target.
      *
      * @param validTargets An array of array. First level array contains conditions that must all be matched (AND). Inner array contains type names, one of them must be matched (OR).
      * @param candidateNodeTypeName Name of the type of the node that is candidate.
      * @param candidateCapabilityIndex Index of the capability of the candidate node that we should check as valid target.
      * @param nodeTypes A map of all available node types (it must contains the candidate node type).
      * @param capabilityTypes A map of all available capability types (it must contains the candidate capability type).
      * @return true if the capability type is one of the valid targets, false if not.
      */
      isValidTarget: function(validTargets, candidateNodeTypeName, candidateCapabilityIndex, nodeTypes, capabilityTypes) {
        var isValid = true;
        // we should match all valid targets.
        for(var i=0; i<validTargets.length; i++) {
          // One of the validTargets[i] element should be either the capability type or the node type.
          isValid = isValid && (toscaService.isOneOfType(validTargets[i], candidateNodeTypeName, nodeTypes) ||
            toscaService.isOneOfType(validTargets[i], nodeTypes[candidateNodeTypeName].capabilities[candidateCapabilityIndex].type, capabilityTypes));
        }
        return isValid;
      }
    };
  }
]);
