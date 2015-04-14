/* global UTILS */
'use strict';

angular.module('alienUiApp').factory('simpleNodeRendererService', ['commonNodeRendererService', 'toscaService',
  function(commonNodeRendererService, toscaService) {
    return {

      isRuntime: false,
      width: 50,
      height: 60,
      distanceBetweenBranchHorizontal: 40,
      distanceBetweenNodeHorizontal: 20,
      distanceBetweenNodeVertical: 20,

      setRuntime: function(isRuntime) {
        this.isRuntime = isRuntime;
        if (isRuntime) {
          this.height = 95;
        } else {
          this.height = 60;
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

      drawRuntimeInfos: function(runtimeGroup, nodeInstances, nodeInstancesCount, rectOriginX, rectOriginY) {
        var currentY = rectOriginY + 40;
        var deletedCount = 0;
        if (UTILS.isDefinedAndNotNull(nodeInstances) && nodeInstancesCount > 0) {
          //the deployment status is no more unknown
          this.removeRuntimeCount(runtimeGroup, 'runtime-count-unknown');

          var successCount = this.getNumberOfInstanceByStatus(nodeInstances, 'SUCCESS');
          var failureCount = this.getNumberOfInstanceByStatus(nodeInstances, 'FAILURE');
          deletedCount = this.getNumberOfInstanceByStatus(nodeInstances, null);

          if (nodeInstancesCount === successCount) {
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 25).attr('cy', rectOriginY + 75).attr('r', '16')
              .attr('orient', 'auto').attr('style', 'stroke: none; fill: green');
          } else if (nodeInstancesCount === failureCount) {
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 25).attr('cy', rectOriginY + 75).attr('r', '16')
              .attr('orient', 'auto').attr('style', 'stroke: none; fill: red');
          } else if (nodeInstancesCount === deletedCount) {
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 25).attr('cy', rectOriginY + 75).attr('r', '16').attr(
              'orient', 'auto').attr('style', 'stroke: none; fill: gray');
          } else {
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 25).attr('cy', rectOriginY + 75).attr('r', '16')
              .attr('orient', 'auto').attr('style', 'stroke: none; fill: orange');
          }
        } else {
          //unknown status
          this.drawRuntimeCount(runtimeGroup, 'runtime-count-unknown', rectOriginX, currentY, '\uf110');
          runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 25).attr('cy', rectOriginY + 75).attr('r', '16').attr(
            'orient', 'auto').attr('style', 'stroke: none; fill: gray');
        }

        // draw the instance count at good size
        runtimeGroup = commonNodeRendererService.appendCount(runtimeGroup, nodeInstancesCount, deletedCount, rectOriginX, rectOriginY, 30, 80, this.width);
      },

      updateNode: function(nodeGroup, node, nodeTemplate, nodeType, oX, oY, topology) {
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

        // runtime infos
        if (this.isRuntime) {
          var runtimeGroup = nodeGroup.select('#runtime');

          var nodeInstances = null;
          var nodeInstancesCount = null;
          var nodeScalingPolicies = null;

          if (UTILS.isDefinedAndNotNull(topology.instances)) {
            nodeInstances = topology.instances[node.id];
            if (UTILS.isDefinedAndNotNull(nodeInstances)) {
              nodeInstancesCount = Object.keys(nodeInstances).length;
              nodeScalingPolicies = topology.topology.scalingPolicies[node.id];
            }
          }
          // TODO better draw network node
          if (!toscaService.isOneOfType(['tosca.nodes.Network'], nodeTemplate.type, topology.nodeTypes)) {
            this.drawRuntimeInfos(runtimeGroup, nodeInstances, nodeInstancesCount, oX, oY);
          }
        }
      },

      // common services
      removeRuntimeCount: commonNodeRendererService.removeRuntimeCount,
      getNumberOfInstanceByStatus: commonNodeRendererService.getNumberOfInstanceByStatus,
      tooltip: commonNodeRendererService.tooltip
    };
  } // function
]);
