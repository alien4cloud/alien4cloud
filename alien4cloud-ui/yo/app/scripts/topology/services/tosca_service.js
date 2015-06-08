/* global UTILS */
'use strict';

angular.module('alienUiApp').factory('toscaService',
  function() {
    var containerType = 'tosca.capabilities.Container';
    var hostedOnType = 'tosca.relationships.HostedOn';
    var networkType = 'tosca.relationships.Network';
    var attachedToType = 'tosca.relationships.AttachTo';

    return {
      getScalingPolicy: function(node) {
        if (UTILS.isDefinedAndNotNull(node.capabilitiesMap) && UTILS.isDefinedAndNotNull(node.capabilitiesMap['scalable'])) {
          var scalableCapability = node.capabilitiesMap['scalable'].value;
          var min = parseInt(scalableCapability.propertiesMap['min_instances'].value.value);
          var max = parseInt(scalableCapability.propertiesMap['max_instances'].value.value);
          var init = parseInt(scalableCapability.propertiesMap['default_instances'].value.value);
          if (min > 1 || max > 1 || init > 1) {
            return {
              minInstances: min,
              maxInstances: max,
              initialInstances: init
            };
          }
        }
      },
      /**
       * Return the simple name of a TOSCA element from it's complex name.
       *
       * @param name The full name of a TOSCA element.
       * @return The simple name of the element.
       */
      simpleName: function(longName) {
        var tokens = longName.trim().split('.');
        if (tokens.length > 0) {
          return tokens[tokens.length - 1];
        } else {
          return longName;
        }
      },

      /**
       * Checks if a capability type is a Container capability.
       *
       * @param capabilityTypeName The name of the capability type.
       * @param capabilityTypes A map of available capability types. It must contains the actual capabilityTypeName.
       * @return true if the type is a tosca container type.
       */
      isContainerType: function(capabilityTypeName, capabilityTypes) {
        return this.isOneOfType([containerType], capabilityTypeName, capabilityTypes);
      },

      /**
       * Checks if a relationshipType is an instance of hosted on.
       *
       * @param relationshipTypeName The name of the relationship type to check.
       * @param relationshipTypes A map of available relationships types. It must contains the actual relationshipTypeName.
       */
      isHostedOnType: function(relationshipTypeName, relationshipTypes) {
        return this.isOneOfType([hostedOnType], relationshipTypeName, relationshipTypes);
      },

      /**
       * Checks if a relationshipType is an instance of network.
       *
       * @param relationshipTypeName The name of the relationship type to check.
       * @param relationshipTypes A map of available relationships types. It must contains the actual relationshipTypeName.
       */
      isNetworkType: function(relationshipTypeName, relationshipTypes) {
        return this.isOneOfType([networkType], relationshipTypeName, relationshipTypes);
      },

      /**
       * Checks if a relationshipType is an instance of attached to.
       *
       * @param relationshipTypeName The name of the relationship type to check.
       * @param relationshipTypes A map of available relationships types. It must contains the actual relationshipTypeName.
       */
      isAttachedToType: function(relationshipTypeName, relationshipTypes) {
        return this.isOneOfType([attachedToType], relationshipTypeName, relationshipTypes);
      },

      /**
       * Check if a type is a one of requested types.
       *
       * @param requestedTypes Array that contains type names, one of them has to be the candidate type or one of it's parent type.
       * @param candidateTypeName The name of the candidate type.
       * @param typesMap A map of available types that should contains the candidate type.
       * @return true if the type is one of the requested type, false if not.
       */
      isOneOfType: function(requestedTypes, candidateTypeName, typesMap) {
        for (var i = 0; i < requestedTypes.length; i++) {
          var validTarget = requestedTypes[i];
          if (candidateTypeName === validTarget ||
            (UTILS.isDefinedAndNotNull(typesMap[candidateTypeName]) &&
            UTILS.arrayContains(typesMap[candidateTypeName].derivedFrom, validTarget))) {
            return true;
          }
        }
        return false;
      },

      /**
       * Generates a relationship name from a relationship type and a target name.
       *
       * @param type The type of the relationship for which to generate a name.
       * @param targetName The name of the relationship target.
       * @return the generated name of the relationship.
       */
      generateRelationshipName: function(type, targetName) {
        return UTILS.lowerCamelCase(this.simpleName(type)) + UTILS.upperCamelCase(targetName);
      },

      /**
       * Get a subset of a node template relationships based on a criteria function.
       *
       * @param nodeTemplate The node template in which to look for relationships.
       * @param criteria The criteria function that should take a relationship in parameter and output a boolean (true if the criteria is met, false if not).
       */
      getRelationships: function(nodeTemplate, criteria) {
        var founds = [];
        if (UTILS.isUndefinedOrNull(nodeTemplate.relationships)) {
          return founds;
        }

        for (var i = 0; i < nodeTemplate.relationships.length; i++) {
          var relationship = nodeTemplate.relationships[i].value;
          relationship.id = nodeTemplate.relationships[i].key;
          if (criteria(relationship)) {
            founds.push(relationship);
          }
        }

        return founds;
      }
    };
  } // function
);
