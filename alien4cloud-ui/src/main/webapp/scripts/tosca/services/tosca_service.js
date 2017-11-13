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
      var dockerType = 'tosca.nodes.Container.Application.DockerContainer';

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

        getTag: function(tagName, tags) {
          return _.get(_.find(tags, {name:tagName}),'value');
        },

        /**
        * Return the icon from a TOSCA element's tags.
        *
        * @param tags The list of tags.
        * @return the value of the icon tag.
        */
        getIcon: function(tags) {
          return  _.get(_.find(tags, {name:'icon'}),'value');
        },

        /**
        * Return the icon from a TOSCA element's tags.
        *
        * @param element The element for which to retrieve the icon.
        * @return the value of the icon tag.
        */
        getElementIcon: function(element) {
          return this.getIcon(_.get(element, 'tags'));
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
        * Checks if a node type is a Compute node.
        *
        * @param nodeTypeName The name of the node type.
        * @param nodeTypes A map of available node types. It must contains the actual nodeTypeName.
        * @return true if the type is a tosca compute type.
        */
        isDockerType: function(nodeTypeName, nodeTypes) {
          return this.isOneOfType([dockerType], nodeTypeName, nodeTypes);
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
        * @param targetedCapabilityName The name of the relationship target's capability.
        * @return the generated name of the relationship.
        */
        generateRelationshipName: function(type, targetName, targetedCapabilityName) {
          return _.camelCase(this.simpleName(type)) + _.capitalize(_.camelCase(targetName)) + _.capitalize(targetedCapabilityName);
        },

        /**
        * Generate a unique template name from the given type name and based on a map of existing templates.
        * @param type The name of the type.
        * @param templates The map of existing templates (to avoid duplicating a template name).
        */
        generateTemplateName: function(type, templates) {
          var baseName = this.simpleName(type);
          // First we have to normalize the node template name as a4c restrict special character usage
          baseName = this.getToscaName(baseName);
          var i = 1;
          var tempName = baseName;
          if(_.defined(templates)) {
            while (templates.hasOwnProperty(tempName)) {
              i++;
              tempName = baseName + '_' + i;
            }
          }
          return tempName;
        },

        toscaNamePattern: /^\w+$/,
        toscaNameReplacePattern: /\W/g,
        /**
        * Get the name in a format that is accepted by alien4cloud.
        */
        getToscaName: function(name) {
          return this.toscaNamePattern.test(name) ? name : name.replace(this.toscaNameReplacePattern, '_');
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
        },

        /**
        * Get all hosted on relationships on a given node template.
        */
        getHostedOnRelationships: function(nodeTemplate, relationshipTypes) {
          var self = this;
          var hostedOnRelationships = self.getRelationships(nodeTemplate, function(relationship) {
            return self.isHostedOnType(relationship.type, relationshipTypes);
          });
          return hostedOnRelationships;
        },

        /**
        * Get all attached to relationships on a given node template.
        */
        getAttachedToRelationships: function(nodeTemplate, relationshipTypes) {
          var self = this;
          var relationships = self.getRelationships(nodeTemplate, function(relationship) {
            return self.isAttachedToType(relationship.type, relationshipTypes);
          });
          return relationships;
        },

        /**
        * Get all attached to relationships on a given node template.
        */
        getNetworkRelationships: function(nodeTemplate, relationshipTypes) {
          var self = this;
          var relationships = self.getRelationships(nodeTemplate, function(relationship) {
            return self.isNetworkType(relationship.type, relationshipTypes);
          });
          return relationships;
        },

        /**
        * Get all depends on to relationships on a given node template.
        */
        getDependsOnRelationships: function(nodeTemplate, relationshipTypes) {
          var self = this;
          var dependsOnRelationships = self.getRelationships(nodeTemplate, function(relationship) {
            return !self.isHostedOnType(relationship.type, relationshipTypes) && !self.isNetworkType(relationship.type, relationshipTypes);
          });
          return dependsOnRelationships;
        }
      };
    } // function
  ); // factory
}); // define
