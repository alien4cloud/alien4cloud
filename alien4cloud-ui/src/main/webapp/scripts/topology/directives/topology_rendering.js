// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var d3 = require('d3');

  require('scripts/common-graph/services/svg_service');
  require('scripts/topology/services/topology_svg_service');
  require('scripts/topology/services/simple_node_renderer_service');
  require('scripts/topology/services/default_node_renderer_service');

  modules.get('a4c-topology-editor').directive('topologyRendering',
    ['topologySvgFactory', 'simpleNodeRendererService', 'defaultNodeRendererService',
    function(topologySvgFactory, simpleNodeRendererService, defaultNodeRendererService) {
      return {
        restrict: 'E',
        scope: {
          selectCallback: '&',
          addRelationshipCallback: '&',
          topology: '=',
          dimensions: '=',
          runtime: '=',
          simple: '='
        },
        link: function(scope, element) {
          // Default parent svg markup to render the topology
          var topologyElement = d3.select(element[0]);

          function getNodeRenderer() {
            var nodeRendererService = scope.simple === true ? simpleNodeRendererService : defaultNodeRendererService;
            if (_.defined(scope.runtime)) {
              nodeRendererService.setRuntime(scope.runtime);
            }
            return nodeRendererService;
          }

          var callbacks = {
            click: scope.selectCallback,
            addRelationship: scope.addRelationshipCallback
          };
          var nodeRenderer = getNodeRenderer();
          var topologySvg = topologySvgFactory.create(callbacks, topologyElement, scope.runtime, nodeRenderer);

          scope.$watch('topology', function(topology) {
            topologySvg.reset(topology);
          }, true);

          scope.$watch('dimensions', function(dimensions) {
            topologySvg.onResize(dimensions);
          });

          scope.$watch('simple', function() {
            topologySvg.setNodeRenderer(getNodeRenderer());
          });
        }
      };
    }
  ]); // directive
}); // define
