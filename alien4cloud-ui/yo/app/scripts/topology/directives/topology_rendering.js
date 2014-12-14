/* global d3 */

'use strict';

angular.module('alienUiApp').directive(
  'topologyRendering', ['topologySvgFactory', 'defaultNodeRendererService', 'simpleNodeRendererService',
    function(topologySvgFactory, defaultNodeRendererService, simpleNodeRendererService) {
      return {
        restrict : 'E',
        scope : {
          callback: '&',
          topology: '=',
          dimensions: '=',
          runtime: '=',
          simple: '='
        },
        link : function(scope, element) {
          // Default parent svg markup to render the topology
          var topologyElement = d3.select(element[0]);

          function getNodeRenderer() {
            if(scope.runtime || !scope.simple) {
              defaultNodeRendererService.setRuntime(scope.runtime);
              return defaultNodeRendererService;
            }
            return simpleNodeRendererService;
          }

          var nodeRenderer = getNodeRenderer();

          var topologySvg = topologySvgFactory.create(scope.callback, topologyElement, scope.runtime, nodeRenderer);

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
  ]);
