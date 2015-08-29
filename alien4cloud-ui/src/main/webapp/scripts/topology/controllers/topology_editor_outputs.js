/** Management of outputs in a topology. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('topoEditOutputs', [ 'topologyServices',
    function(topologyServices) {

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
          var nodeTemplateName = scope.selectedNodeTemplate.name;
          var topology = scope.topology.topology;
          var params = {
            topologyId: scope.topology.topology.id,
            nodeTemplateName: nodeTemplateName
          };

          if (outputType === 'outputProperties') {
            params.propertyName = propertyName;
          }
          if (outputType === 'outputAttributes') {
            params.attributeName = propertyName;
          }

          var inputIndex = topology[outputType][nodeTemplateName].indexOf(propertyName);

          if (inputIndex < 0) {
            // add input property
            topologyServices.nodeTemplate[outputType].add(
              params,
              function(successResult) {
                if (!successResult.error) {
                  scope.refreshTopology(successResult.data, scope.selectedNodeTemplate ? scope.selectedNodeTemplate.name : undefined);
                } else {
                  console.debug(successResult.error);
                }
              },
              function(errorResult) {
                console.debug(errorResult);
              }
            );
          } else {
            // remove input
            topologyServices.nodeTemplate[outputType].remove(
              params,
              function(successResult) {
                if (!successResult.error) {
                  scope.refreshTopology(successResult.data, scope.selectedNodeTemplate ? scope.selectedNodeTemplate.name : undefined);
                } else {
                  console.debug(successResult.error);
                }
              },
              function(errorResult) {
                console.debug(errorResult);
              }
            );
          }
        },

        toggleCapabilityOutput: function(capabilityId, propertyId) {
          var scope = this.scope;
          var nodeTemplateName = scope.selectedNodeTemplate.name;
          var topology = scope.topology.topology;
          var inputIndex = -1;

          if (_.defined(topology.outputCapabilityProperties) &&
            _.defined(topology.outputCapabilityProperties[nodeTemplateName]) &&
            _.defined(topology.outputCapabilityProperties[nodeTemplateName][capabilityId])) {
            inputIndex = topology.outputCapabilityProperties[nodeTemplateName][capabilityId].indexOf(propertyId);
          }

          var params = {
            topologyId: scope.topology.topology.id,
            nodeTemplateName: nodeTemplateName,
            capabilityId: capabilityId,
            propertyId: propertyId
          };

          if (inputIndex < 0) {
            // add input property
            topologyServices.nodeTemplate.capability.outputProperties.add(
              params,
              function(successResult) {
                if (!successResult.error) {
                  scope.refreshTopology(successResult.data, scope.selectedNodeTemplate ? scope.selectedNodeTemplate.name : undefined);
                } else {
                  console.debug(successResult.error);
                }
              },
              function(errorResult) {
                console.debug(errorResult);
              }
            );
          } else {
            // remove input
            topologyServices.nodeTemplate.capability.outputProperties.remove(
              params,
              function(successResult) {
                if (!successResult.error) {
                  scope.refreshTopology(successResult.data, scope.selectedNodeTemplate ? scope.selectedNodeTemplate.name : undefined);
                } else {
                  console.debug(successResult.error);
                }
              },
              function(errorResult) {
                console.debug(errorResult);
              }
            );
          }
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
