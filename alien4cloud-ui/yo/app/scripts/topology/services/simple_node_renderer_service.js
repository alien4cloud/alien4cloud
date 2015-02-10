/* global UTILS */
'use strict';

angular.module('alienUiApp').factory('simpleNodeRendererService', ['commonNodeRendererService',
  function(commonNodeRendererService) {
    return {
      isRuntime: false,
      width: 50,
      height: 70,
      distanceBetweenBranchHorizontal: 50,
      distanceBetweenNodeHorizontal: 30,
      distanceBetweenNodeVertical: 40,

      setRuntime: function(isRuntime) {
        this.isRuntime = isRuntime;
        if (isRuntime) {
          this.height = 100;
        } else {
          this.height = 70;
        }
      },

      createNode: function(nodeGroup, node, nodeTemplate, nodeType, oX, oY) {
        if (nodeType.tags) {
          var tags = UTILS.convertNameValueListToMap(nodeType.tags);
          if (tags.icon) {
            nodeGroup.append('image').attr('x', oX + 10).attr('y', oY + 8).attr('width', '32').attr('height', '32').attr('xlink:href',
              'img?id=' + tags.icon + '&quality=QUALITY_32');
          }
        }

        nodeGroup.append('text').attr('text-anchor', 'start').attr('class', 'simple-title small').attr('x', oX + 2).attr('y', oY + 52);
        nodeGroup.append('text').attr('text-anchor', 'start').attr('class', 'simple-version small').attr('x', oX + 2).attr('y', oY + 66);

        // specific to the runtime view
        if (this.isRuntime) {
          nodeGroup.append('g').attr('id', 'runtime');
        }
      },

      updateNode: function(nodeGroup, node, nodeTemplate, nodeType, oX, oY) {
        // update text
        nodeGroup.select('.simple-title').text(commonNodeRendererService.getDisplayId(node, true));

        // update version
        nodeGroup.select('.simple-version').text(function() {
          if (nodeTemplate.properties) {
            if (typeof nodeTemplate.properties.version === 'string') {
              return 'v' + nodeTemplate.properties.version;
            }
          }
        });
      },
      tooltip: commonNodeRendererService.tooltip,




      drawRuntimeInfos: function(runtimeGroup, nodeInstances, nodeInstancesCount, rectOriginX, rectOriginY) {
        console.log('Draw simple runtime info');
        var currentY = rectOriginY + 40;
        var deletedCount = 0;
        if (UTILS.isDefinedAndNotNull(nodeInstances) && nodeInstancesCount > 0) {
          //the deployment status is no more unknown
          this.removeRuntimeCount(runtimeGroup, 'runtime-count-unknown');

          var successCount = this.getNumberOfInstanceByStatus(nodeInstances, 'SUCCESS');
          var processingCount = this.getNumberOfInstanceByStatus(nodeInstances, 'PROCESSING');
          var failureCount = this.getNumberOfInstanceByStatus(nodeInstances, 'FAILURE');
          deletedCount = this.getNumberOfInstanceByStatus(nodeInstances, null);
          if (successCount > 0) {
            commonNodeRendererService.drawRuntimeCount(runtimeGroup, 'runtime-count-success', rectOriginX, currentY, '\uf00c', successCount, nodeInstancesCount);
            currentY += 20;
          } else {
            this.removeRuntimeCount(runtimeGroup, 'runtime-count-success');
          }
          if (processingCount > 0) {
            commonNodeRendererService.drawRuntimeCount(runtimeGroup, 'runtime-count-progress', rectOriginX, currentY, '\uf110', processingCount, nodeInstancesCount);
            currentY += 20;
          } else {
            this.removeRuntimeCount(runtimeGroup, 'runtime-count-progress');
          }
          if (failureCount > 0) {
            commonNodeRendererService.drawRuntimeCount(runtimeGroup, 'runtime-count-failure', rectOriginX, currentY, '\uf00d', failureCount, nodeInstancesCount);
          } else {
            this.removeRuntimeCount(runtimeGroup, 'runtime-count-failure');
          }

          if (nodeInstancesCount === successCount) {
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 17).attr('cy', rectOriginY + 16).attr('r', '12')
              .attr('orient', 'auto').attr('style', 'stroke: none; fill: green');
          } else if (nodeInstancesCount === failureCount) {
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 17).attr('cy', rectOriginY + 16).attr('r', '12')
              .attr('orient', 'auto').attr('style', 'stroke: none; fill: red');
          } else if (nodeInstancesCount === deletedCount) {
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 17).attr('cy', rectOriginY + 16).attr('r', '12').attr(
              'orient', 'auto').attr('style', 'stroke: none; fill: gray');
          } else {
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 17).attr('cy', rectOriginY + 16).attr('r', '12')
              .attr('orient', 'auto').attr('style', 'stroke: none; fill: orange');
          }
        } else {
          //unknown status
          commonNodeRendererService.drawRuntimeCount(runtimeGroup, 'runtime-count-unknown', rectOriginX, currentY, '\uf110');
          runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 17).attr('cy', rectOriginY + 16).attr('r', '12').attr(
            'orient', 'auto').attr('style', 'stroke: none; fill: gray');
        }

        runtimeGroup.append('text').attr('text-anchor', 'start').attr('x', rectOriginX + this.width - 20).attr('y', rectOriginY + 20).attr('font-weight',
          'bold').attr('fill', 'white').text(function() {
          return nodeInstancesCount ? nodeInstancesCount - deletedCount : 0;
        });
      },

      removeRuntimeCount: function(runtimeGroup, id) {
        var groupSelection = runtimeGroup.select('#' + id);
        if (!groupSelection.empty()) {
          groupSelection.remove();
        }
      },

      getNumberOfInstanceByStatus: function(nodeInstances, status) {
        var count = 0;
        for (var instanceId in nodeInstances) {
          if (nodeInstances.hasOwnProperty(instanceId) && nodeInstances[instanceId].instanceStatus === status) {
            count++;
          }
        }
        return count;
      }



    };
  } // function
]);
