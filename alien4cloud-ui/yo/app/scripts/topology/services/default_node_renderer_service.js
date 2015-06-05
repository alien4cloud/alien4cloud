/* global UTILS */
'use strict';

angular.module('alienUiApp').factory('defaultNodeRendererService', ['commonNodeRendererService', 'toscaService',
  function(commonNodeRendererService, toscaService) {
    return {

      isRuntime: false,
      width: 200,
      height: 50,
      distanceBetweenBranchHorizontal: 40,
      distanceBetweenNodeHorizontal: 20,
      distanceBetweenNodeVertical: 20,

      setRuntime: function(isRuntime) {
        this.isRuntime = isRuntime;
        if (isRuntime) {
          this.height = 85;
        } else {
          this.height = 50;
        }
      },

      createNode: function(nodeGroup, node, nodeTemplate, nodeType, oX, oY) {
        if (nodeType.tags) {
          var tags = UTILS.convertNameValueListToMap(nodeType.tags);
          if (tags.icon) {
            nodeGroup.append('image').attr('x', oX + 8).attr('y', oY + 8).attr('width', '32').attr('height', '32').attr('xlink:href',
              'img?id=' + tags.icon + '&quality=QUALITY_32');
          }
        }

        if (nodeType.abstract) {
          var icoSize = 16;
          var x = 80;
          var y = -22;
          nodeGroup.append('image').attr('x', x).attr('y', y).attr('width', icoSize).attr('height', icoSize).attr('xlink:href', 'images/abstract_ico.png');
        }

        nodeGroup.append('text').attr('text-anchor', 'start').attr('class', 'title').attr('x', oX + 44).attr('y', oY + 20);
        nodeGroup.append('text').attr('text-anchor', 'end').attr('class', 'version').attr('x', '80').attr('y', '20');

        // specific to the runtime view
        if (this.isRuntime) {
          nodeGroup.append('g').attr('id', 'runtime');
        }
      },

      updateNode: function(nodeGroup, node, nodeTemplate, nodeType, oX, oY, topology) {
        // update text
        nodeGroup.select('.title').text(commonNodeRendererService.getDisplayId(node, false));

        // update version
        nodeGroup.select('.version').text(function() {
          if (UTILS.isDefinedAndNotNull(nodeTemplate.properties) && UTILS.isDefinedAndNotNull(nodeTemplate.properties.version)) {
            return 'v' + nodeTemplate.properties.version.value;
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
              nodeScalingPolicies = toscaService.getScalingPolicy(node);
            }
          }

          // TODO better draw network node
          if (!toscaService.isOneOfType(['tosca.nodes.Network'], nodeTemplate.type, topology.nodeTypes)) {
            this.drawRuntimeInfos(runtimeGroup, nodeInstances, nodeInstancesCount, oX, oY, nodeScalingPolicies);
          }
        }

        if (UTILS.isDefinedAndNotNull(nodeTemplate.groups)) {
          var gIdx = 1;
          // group square width is calculated using the node width (1/15)
          var gW = this.width / 15;
          if (nodeTemplate.groups.length > 10) {
            // reduce the size of bullets when too many groups
            gW = this.width / (nodeTemplate.groups.length + 10);
          }
          // group square height is calculated using the node height (about 1/2)
          var gH = this.height / 2.5;
          // the group y is near the 2/3 of the height of the node
          var gY = oY + (2.2 * this.height / 3);
          // the end of the node square
          var nodeEndX = oX + this.width - (gW / 2);
          angular.forEach(nodeTemplate.groups, function(value, key) {
            // let's place the group square regarding it's index and applying a 0.2 margin
            var gX = nodeEndX - (gIdx * 1.2 * gW);

            var rect = D3JS_UTILS.rect(nodeGroup, gX, gY, gW, gH, 3, 3).attr('class', 'node-template-group ' + D3JS_UTILS.groupColorCss(topology.topology, value));
            // add the group name as title (for popping over)
            rect.attr('title', value);

            gIdx++;
          });
        }

      },

      drawRuntimeInfos: function(runtimeGroup, nodeInstances, nodeInstancesCount, rectOriginX, rectOriginY, scalingPolicies) {
        var currentY = rectOriginY + 40;
        var deletedCount = 0;
        if (UTILS.isDefinedAndNotNull(nodeInstances) && nodeInstancesCount > 0) {
          //the deployment status is no more unknown
          this.removeRuntimeCount(runtimeGroup, 'runtime-count-unknown');

          var successCount = this.getNumberOfInstanceByStatus(nodeInstances, 'SUCCESS');
          var processingCount = this.getNumberOfInstanceByStatus(nodeInstances, 'PROCESSING');
          var maintenanceCount = this.getNumberOfInstanceByStatus(nodeInstances, 'MAINTENANCE');
          var failureCount = this.getNumberOfInstanceByStatus(nodeInstances, 'FAILURE');
          deletedCount = this.getNumberOfInstanceByStatus(nodeInstances, null, 'stopped');

          // adapt instance count
          if (UTILS.isDefinedAndNotNull(scalingPolicies)) {
            nodeInstancesCount = scalingPolicies.initialInstances;
          }

          if (successCount > 0) {
            this.drawRuntimeCount(runtimeGroup, 'runtime-count-success', rectOriginX, currentY, '\uf00c', successCount, nodeInstancesCount);
            currentY += 20;
          } else {
            this.removeRuntimeCount(runtimeGroup, 'runtime-count-success');
          }
          if (processingCount > 0) {
            this.drawRuntimeCount(runtimeGroup, 'runtime-count-progress', rectOriginX, currentY, '\uf110', processingCount, nodeInstancesCount);
            currentY += 20;
          } else {
            this.removeRuntimeCount(runtimeGroup, 'runtime-count-progress');
          }
          if (maintenanceCount > 0) {
            this.drawRuntimeCount(runtimeGroup, 'runtime-count-maintenance', rectOriginX, currentY, '\uf0ad', maintenanceCount, nodeInstancesCount);
          } else {
            this.removeRuntimeCount(runtimeGroup, 'runtime-count-maintenance');
          }
          if (failureCount > 0) {
            this.drawRuntimeCount(runtimeGroup, 'runtime-count-failure', rectOriginX, currentY, '\uf00d', failureCount, nodeInstancesCount);
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
          this.drawRuntimeCount(runtimeGroup, 'runtime-count-unknown', rectOriginX, currentY, '\uf110');
          runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 17).attr('cy', rectOriginY + 16).attr('r', '12').attr(
            'orient', 'auto').attr('style', 'stroke: none; fill: gray');
        }

        // draw the instance count at good size
        runtimeGroup = commonNodeRendererService.appendCount(runtimeGroup, nodeInstancesCount, deletedCount, rectOriginX, rectOriginY, 20, 20, this.width);
      },

      drawRuntimeCount: function(runtimeGroup, id, rectOriginX, currentY, iconCode, count, instanceCount) {
        var groupSelection = runtimeGroup.select('#' + id);
        // improve that...
        var counter = (count || '?') + '/' + (instanceCount || '?');
        if (groupSelection.empty()) {
          groupSelection = runtimeGroup.append('g').attr('id', id);
          groupSelection.append('text').attr('class', 'topology-svg-icon').attr('text-anchor', 'start').attr('x', rectOriginX + 60).attr('y', currentY).text(iconCode);
          groupSelection.append('text').attr('id', 'count-text').attr('text-anchor', 'start').attr('x', rectOriginX + 80).attr('y', currentY).text(counter);
        } else {
          groupSelection.select('#count-text').text(counter);
        }
      },

      // common services
      removeRuntimeCount: commonNodeRendererService.removeRuntimeCount,
      getNumberOfInstanceByStatus: commonNodeRendererService.getNumberOfInstanceByStatus,
      tooltip: commonNodeRendererService.tooltip
    };
  } // function
]);
