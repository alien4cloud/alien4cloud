/** Management of outputs in a topology. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var angular = require('angular');

  modules.get('a4c-topology-editor').factory('topoEditOutputs', [
    function() {

      var outputKeys = ['outputProperties', 'outputAttributes'];

      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        // Add property / artifact / attributes to input list
        initOutputMap: function(topology, nodeTemplateName, key) {
          if (angular.isDefined(topology[key])) {
            if (!angular.isDefined(topology[key][nodeTemplateName])) {
              topology[key][nodeTemplateName] = [];
            }
          } else {
            topology[key] = {};
            topology[key][nodeTemplateName] = [];
          }
        },

        initOutputMaps: function(topology, nodeTemplateName) {
          var instance = this;
          outputKeys.forEach(function(key) {
            instance.initOutputMap(topology, nodeTemplateName, key);
          });
        },

        init: function(topology) {
          var instance = this;
          if (topology.nodeTemplates) {
            Object.keys(topology.nodeTemplates).forEach(function(nodeTemplateName) {
              instance.initOutputMaps(topology, nodeTemplateName);
            });
          }
        },

        toggleOutput: function(propertyName, outputType) {
          var scope = this.scope;
          var operation = {
            nodeName: scope.selectedNodeTemplate.name
          };

          switch (outputType) {
            case 'outputProperties':
              operation.propertyName = propertyName;
              operation.type = scope.properties.isOutputProperty(propertyName)?
                                 'org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.UnSetNodePropertyAsOutputOperation'
                                :'org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodePropertyAsOutputOperation';
              break;
            case 'outputAttributes':
              operation.attributeName = propertyName;
              operation.type = scope.properties.isOutputAttribute(propertyName)?
                                 'org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.UnSetNodeAttributeAsOutputOperation'
                                :'org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeAttributeAsOutputOperation';
              break;
            default:
          }

          // add/remove output
          this.scope.execute(operation);

        },

        toggleOutputCapabilityProperty: function(capabilityName, propertyName) {
          var scope = this.scope;
          var operation = {
            nodeName: scope.selectedNodeTemplate.name,
            propertyName: propertyName,
            capabilityName: capabilityName
          };

          operation.type = scope.properties.isOutputCapabilityProperty(capabilityName,propertyName)?
                             'org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.UnSetNodeCapabilityPropertyAsOutputOperation'
                            :'org.alien4cloud.tosca.editor.operations.nodetemplate.outputs.SetNodeCapabilityPropertyAsOutputOperation';

          // add/remove output
          this.scope.execute(operation);
        },

        toggleOutputProperty: function(propertyName) {
          this.toggleOutput(propertyName, 'outputProperties', 'property');
        },

        toggleOutputAttribute: function(attributeName) {
          this.toggleOutput(attributeName, 'outputAttributes', 'attribute');
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.outputs = instance;
      };
    }
  ]); // modules
}); // define
