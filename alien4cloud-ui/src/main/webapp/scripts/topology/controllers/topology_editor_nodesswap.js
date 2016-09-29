/**
*  Service that provides functionalities to replace nodes in a topology.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');


  modules.get('a4c-topology-editor').factory('topoEditNodesSwap', [ '$alresource',
    function($alresource) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      var nodeTemplateReplacementHelperResource = $alresource('rest/latest/editor/:topologyId/nodetemplates/:nodeTemplateName/replacementhelper');

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,

        getPossibleReplacements: function() {
          var self = this;
          nodeTemplateReplacementHelperResource.get({
            topologyId: self.scope.topology.topology.id,
            nodeTemplateName: self.scope.selectedNodeTemplate.name
          }, function(result) {
            self.scope.currentInputCandidatesForCapabilityProperty = result.data;
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
