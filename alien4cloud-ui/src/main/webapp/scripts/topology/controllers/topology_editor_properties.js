/**
 *  Service that provides functionalities to edit nodes in a topology.
 */
define(function(require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('topoEditProperties',
    function() {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        isInputPropertyValue: function(propertyValue) {
          if (_.undefined(propertyValue)) {
            return false;
          }
          return _.defined(propertyValue.function) && propertyValue.function === 'get_input';
        },

        isInputProperty: function(propertyName) {
          var scope = this.scope;
          var propertyValue = scope.selectedNodeTemplate.propertiesMap[propertyName].value;
          return this.isInputPropertyValue(propertyValue);
        },

        isInputRelationshipProperty: function(relationshipName, propertyName) {
          var scope = this.scope;
          var propertyValue = scope.selectedNodeTemplate.relationshipsMap[relationshipName].value.propertiesMap[propertyName].value;
          return this.isInputPropertyValue(propertyValue);
        },

        isOutputProperty: function(propertyName) {
          var scope = this.scope;
          if (_.undefined(scope.topology.topology.outputProperties)) {
            return false;
          }
          return scope.topology.topology.outputProperties[scope.selectedNodeTemplate.name].indexOf(propertyName) >= 0;
        },

        isOutputCapabilityProperty: function(capabilityId, propertyId) {
          var scope = this.scope;
          if (_.undefined(scope.topology.topology.outputCapabilityProperties) || _.undefined(scope.topology.topology.outputCapabilityProperties[scope.selectedNodeTemplate.name]) || _.undefined(scope.topology.topology.outputCapabilityProperties[scope.selectedNodeTemplate.name][capabilityId])) {
            return false;
          }
          return scope.topology.topology.outputCapabilityProperties[scope.selectedNodeTemplate.name][capabilityId].indexOf(propertyId) >= 0;
        },

        isOutputAttribute: function(attributeName) {
          var scope = this.scope;
          if (_.undefined(scope.topology.topology.outputAttributes)) {
            return false;
          }
          return scope.topology.topology.outputAttributes[scope.selectedNodeTemplate.name].indexOf(attributeName) >= 0;
        },

        isSecretValue: function(propertyValue) {
          return _.defined(propertyValue) && _.defined(propertyValue.function) && propertyValue.function === 'get_secret';
        },
        //
        // isSecretProperty: function(propertyName) {
        //   var scope = this.scope;
        //   if (_.undefined(scope.topology.nodeTypes[scope.selectedNodeTemplate.type].propertiesMap[propertyName].value.secret)) {
        //     scope.topology.nodeTypes[scope.selectedNodeTemplate.type].propertiesMap[propertyName].value.secret = false;
        //   }
        //   return scope.topology.nodeTypes[scope.selectedNodeTemplate.type].propertiesMap[propertyName].value.secret;
        // },

        // isSecretCapabilityProperty: function(propertyName) {
        //   var scope = this.scope;
        //   if (_.undefined(scope.topology.nodeTypes[scope.selectedNodeTemplate.type].capabilitiesMap[propertyName].value.secret)) {
        //     scope.topology.nodeTypes[scope.selectedNodeTemplate.type].capabilitiesMap[propertyName].value.secret = false;
        //   }
        //   return scope.topology.nodeTypes[scope.selectedNodeTemplate.type].capabilitiesMap[propertyName].value.secret;
        // },

        getFormatedProperty: function(propertyKey) {
          var scope = this.scope;
          return scope.topology.nodeTypes[scope.selectedNodeTemplate.type].propertiesMap[propertyKey].value;
        },

        getFormatedCapabilityProperty: function(capability, propertyKey) {
          var scope = this.scope;
          return scope.topology.capabilityTypes[capability].propertiesMap[propertyKey].value;
        },

        getPropertyDescription: function(propertyKey) {
          var scope = this.scope;
          return scope.topology.nodeTypes[scope.selectedNodeTemplate.type].propertiesMap[propertyKey].value.description;
        },

        /* Update properties of a capability */
        updateCapabilityProperty: function(propertyName, propertyValue, capabilityType, capabilityId) {
          var scope = this.scope;
          return scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateCapabilityPropertyValueOperation',
              nodeName: scope.selectedNodeTemplate.name,
              capabilityName: capabilityId,
              propertyName: propertyName,
              propertyValue: propertyValue
            },
            function(result) {
              if (_.undefined(result.error)) {
                scope.topology.topology.nodeTemplates[scope.selectedNodeTemplate.name].capabilitiesMap[capabilityId].value.propertiesMap[propertyName].value = { value: propertyValue, definition: false };
                if (capabilityType === 'tosca.capabilities.Scalable') {
                  // This is the only property with the version that updates the rendering
                  scope.$broadcast('editorUpdateNode', { node: scope.selectedNodeTemplate.name });
                }
              }
            },
            null,
            scope.selectedNodeTemplate.name,
            true
          );
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.properties = instance;
      };
    }
  ); // modules
}); // define
