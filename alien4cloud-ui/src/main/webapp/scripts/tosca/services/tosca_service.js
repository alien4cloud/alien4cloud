define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-tosca').factory('toscaService',
    function() {
      var containerType = 'tosca.capabilities.Container';
      var hostedOnType = 'tosca.relationships.HostedOn';
      var networkType = 'tosca.relationships.Network';
      var attachedToType = 'tosca.relationships.AttachTo';
      var computeType = 'tosca.nodes.Compute';

      var getScalingProperty = function(scalableCapability, propertyName) {
        var propertyEntry = scalableCapability.propertiesMap[propertyName];
        var propertyValue = 1;
        if (_.defined(propertyEntry) && _.defined(propertyEntry.value)) {
          if (_.defined(propertyEntry.value.value)) {
            // Scalar
            propertyValue = parseInt(propertyEntry.value.value);
          } else if (_.defined(propertyEntry.value.function) &&
            _.defined(propertyEntry.value.parameters) &&
            propertyEntry.value.parameters.length > 0) {
            // Get input
            propertyValue = propertyEntry.value;
          }
        }
        return propertyValue;
      };

      return {
        standardInterfaceName: 'tosca.interfaces.node.lifecycle.Standard',

        getScalingPolicy: function(node) {
          if (_.defined(node.capabilitiesMap) && _.defined(node.capabilitiesMap.scalable)) {
            var scalableCapability = node.capabilitiesMap.scalable.value;
            var min = getScalingProperty(scalableCapability, 'min_instances');
            var max = getScalingProperty(scalableCapability, 'max_instances');
            var init = getScalingProperty(scalableCapability, 'default_instances');
            // If min == max == default == 1 we consider that the node is not scalable
            if (min !== 1 || max !== 1 || init !== 1) {
              return {
                minInstances: min,
                maxInstances: max,
                initialInstances: init
              };
            }
          }
        },

        /**
        * Return the icon from a TOSCA element's tags.
        *
        * @param tags The map of tags.
        * @return the value of the icon tag.
        */
        getIcon: function(tags) {
          for ( var i in tags) {
            var tag = tags[i];
            if (tag.name === 'icon') {
              return tag.value;
            }
          }
        },

        /**
        * Return the simple name of a TOSCA element from it's complex name.
        *
        * @param name The full name of a TOSCA element.
        * @return The simple name of the element.
        */
        simpleName : function(longName) {
          var tokens = longName.trim().split('.');
          if (tokens.length > 0) {
            return tokens[tokens.length - 1];
          } else {
            return longName;
          }
        },

        /**
        * Checks if a node type is a Compute node.
        *
        * @param nodeTypeName The name of the node type.
        * @param nodeTypes A map of available node types. It must contains the actual nodeTypeName.
        * @return true if the type is a tosca compute type.
        */
        isComputeType: function(nodeTypeName, nodeTypes) {
          return this.isOneOfType([computeType], nodeTypeName, nodeTypes);
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
          for(var i=0; i<requestedTypes.length; i++) {
            var validTarget = requestedTypes[i];
            if(candidateTypeName === validTarget ||
              (_.defined(typesMap[candidateTypeName]) &&
                _.contains(typesMap[candidateTypeName].derivedFrom, validTarget))) {
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
          return _.camelCase(this.simpleName(type)) + _.capitalize(_.camelCase(targetName));
        },

        nodeTemplatePattern: /^\w+$/,
        nodeTemplateReplacePattern: /\W/g,
        /**
        * Generate a unique node template name from the given node type name and based on a map of existing node templates.
        * @param type The name of the node type.
        * @param nodeTemplates The map of existing node templates (to avoid duplicating a node template name).
        */
        generateNodeTemplateName: function(type, nodeTemplates) {
          var baseName = this.simpleName(type);
          // First we have to normalize the node template name as a4c restrict special character usage
          baseName = this.nodeTemplatePattern.test(baseName) ? baseName : baseName.replace(this.nodeTemplateReplacePattern, '_');
          var i = 1;
          var tempName = baseName;
          if(_.defined(nodeTemplates)) {
            while (nodeTemplates.hasOwnProperty(tempName)) {
              i++;
              tempName = baseName + '_' + i;
            }
          }
          return tempName;
        },

        /**
        * Get a subset of a node template relationships based on a criteria function.
        *
        * @param nodeTemplate The node template in which to look for relationships.
        * @param criteria The criteria function that should take a relationship in parameter and output a boolean (true if the criteria is met, false if not).
        */
        getRelationships: function(nodeTemplate, criteria) {
          var founds = [];
          if (_.undefined(nodeTemplate.relationships)) {
            return founds;
          }

          for(var i=0;i<nodeTemplate.relationships.length;i++) {
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
  ); // factory
}); // define
