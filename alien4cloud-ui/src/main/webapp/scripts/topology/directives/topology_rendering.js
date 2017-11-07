// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var d3 = require('d3');
  var _ = require('lodash');

  require('scripts/common-graph/services/svg_service');
  require('scripts/topology/services/topology_svg_service');
  require('scripts/topology/services/default_node_renderer_service');

  modules.get('a4c-topology-editor').directive('topologyRendering',
    ['topologySvgFactory', 'defaultNodeRendererService',
    function(topologySvgFactory, defaultNodeRendererService) {
      return {
        restrict: 'E',
        scope: {
          callbacks: '=',
          graphControl: '=',
          topology: '=',
          dimensions: '=',
          runtime: '='
        },
        link: function(scope, element) {
          // Default parent svg markup to render the topology
          var topologyElement = d3.select(element[0]);
          var callbacks = {
            click: _.get(scope, 'callbacks.selectNodeTemplate'),
            updateNodePosition: _.get(scope, 'callbacks.updateNodePosition'),
            addRelationship: _.get(scope, 'callbacks.addRelationship'),
            graphControl: scope.graphControl
          };
          defaultNodeRendererService.setRuntime(scope.runtime);
          var topologySvg = topologySvgFactory.create(callbacks, topologyElement, scope.runtime, defaultNodeRendererService);
          var topology = scope.topology;

          scope.$on('topologyRefreshedEvent', function(event, param) {
            topology = param.topology;
            // Draw using d3 js selections the topology updates.
            topologySvg.reset(topology);
          });

          scope.$on('editorSelectionChangedEvent', function(event, param) {
            // just update the selection classes.
            topologySvg.updateNodeSelection(topology, param.nodeNames);
          });

          scope.$on('editorUpdateNode', function() {
            // Just update the given node
            // topologySvg.updateNode(topology, param.node);
            topologySvg.reset(topology);
          });

          scope.$watch('dimensions', function(dimensions) {
            topologySvg.onResize(dimensions);
          });

          topologySvg.reset(topology);
        }
      };
    }
  ]); // directive
}); // define
