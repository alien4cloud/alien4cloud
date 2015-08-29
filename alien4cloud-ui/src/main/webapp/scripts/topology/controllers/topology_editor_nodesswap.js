/**
*  Service that provides functionalities to replace nodes in a topology.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');
  var angular = require('angular');

  modules.get('a4c-topology-editor').factory('topoEditNodesSwap', [ 'topologyServices', 'toscaService',
    function(topologyServices, toscaService) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        getPossibleReplacements: function(selectedNodeTemplate) {
          var scope = this.scope;
          topologyServices.nodeTemplate.getPossibleReplacements({
            topologyId: scope.topology.topology.id,
            nodeTemplateName: selectedNodeTemplate.name
          }, function(result) {
            scope.suggestedReplacements = result.data;
          });
        },

        swapNodeTemplate: function(selectedNodeTemplate, newNodeType) {
          var scope = this.scope;
          var newNodeTemplName = toscaService.generateNodeTemplateName(newNodeType.elementId, scope.topology.topology.nodeTemplates);
          var nodeTemplateRequest = {
            name: newNodeTemplName,
            indexedNodeTypeId: newNodeType.id
          };

          topologyServices.nodeTemplate.replace({
            topologyId: scope.topology.topology.id,
            nodeTemplateName: selectedNodeTemplate.name
          }, angular.toJson(nodeTemplateRequest), function(result) {
            if (!result.error) {
              scope.refreshTopology(result.data, newNodeTemplName);
            }
          });
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.nodesswap = instance;
      };
    }
  ]); // modules
}); // define
