// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var d3 = require('d3');

  require('scripts/common-graph/services/svg_service');
  require('scripts/topology/services/topology_svg_service');
  require('scripts/topology/services/default_node_renderer_service');

  modules.get('a4c-topology-editor').directive('topologyRendering',
    ['topologySvgFactory', 'defaultNodeRendererService',
    function(topologySvgFactory, defaultNodeRendererService) {
      return {
        restrict: 'E',
        scope: {
          selectCallback: '&',
          addRelationshipCallback: '&',
          selectedNodeTemplate: '=', // better add a change callback actually - using select callback ?
          topology: '=',
          dimensions: '=',
          runtime: '='
        },
        link: function(scope, element) {
          // Default parent svg markup to render the topology
          var topologyElement = d3.select(element[0]);
          var callbacks = {
            click: scope.selectCallback,
            addRelationship: scope.addRelationshipCallback
          };
          defaultNodeRendererService.setRuntime(scope.runtime);
          var topologySvg = topologySvgFactory.create(callbacks, topologyElement, scope.runtime, defaultNodeRendererService);

          scope.$watch('topology', function(topology) {
            topologySvg.reset(topology);
          });

          scope.$watch('selectedNodeTemplate', function() {
            console.log('reset topology');
            topologySvg.reset(scope.topology);
          });

          scope.$watch('dimensions', function(dimensions) {
            topologySvg.onResize(dimensions);
          });
        }
      };
    }
  ]); // directive
}); // define
