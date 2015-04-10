/* global d3, UTILS */
'use strict';

angular.module('alienUiApp').directive(
  'topologyRendering', ['topologySvgFactory', 'commonNodeRendererService', 'simpleNodeRendererService', 'defaultNodeRendererService',
    function(topologySvgFactory, commonNodeRendererService, simpleNodeRendererService, defaultNodeRendererService) {
      return {
        restrict: 'E',
        scope: {
          callback: '&',
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
            if (UTILS.isDefinedAndNotNull(scope.runtime)) {
              nodeRendererService.setRuntime(scope.runtime);
            }
            return nodeRendererService;
          }

          var nodeRenderer = getNodeRenderer();
          var topologySvg = topologySvgFactory.create(scope.callback, topologyElement, scope.runtime, nodeRenderer);

          scope.$watch('topology', function(topology) {
            console.log('Topology >', topology);
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
  ]);
