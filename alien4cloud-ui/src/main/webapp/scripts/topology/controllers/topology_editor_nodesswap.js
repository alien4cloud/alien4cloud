/**
*  Service that provides functionalities to replace nodes in a topology.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');


  modules.get('a4c-topology-editor').factory('topoEditNodesSwap', [ 'topologyServices', '$alresource',
    function(topologyServices, $alresource) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      var nodeTemplateReplacementHelperResource = $alresource('rest/latest/editor/:topologyId/nodetemplates/:nodeTemplateName/replacementhelper');

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
          return scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.ReplaceNodeOperation',
              nodeName: scope.selectedNodeTemplate.name,
              newTypeId: newNodeType.id
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
