/* global UTILS */

'use strict';

angular.module('alienUiApp').factory('defaultNodeRendererService', [
  function() {
    return {
      isRuntime: false,
      width: 200,
      height: 50,
      distanceBetweenBranchHorizontal: 80,
      distanceBetweenNodeHorizontal: 40,
      distanceBetweenNodeVertical: 60,

      setRuntime: function(isRuntime) {
        this.isRuntime = isRuntime;
        if (isRuntime) {
          this.height = 100;
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
        nodeGroup.select('.title').text(this.getDisplayId(node));

        // update version
        nodeGroup.select('.version').text(function() {
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


          if (UTILS.isDefinedAndNotNull(topology.instances)) {
            nodeInstances = topology.instances[node.id];
            if (UTILS.isDefinedAndNotNull(nodeInstances)) {
              nodeInstancesCount = Object.keys(nodeInstances).length;
            }
          }
          // TODO better draw network node
          if (nodeType.elementId !== 'tosca.nodes.Network') {
            this.drawRuntimeInfos(runtimeGroup, nodeInstances, nodeInstancesCount, oX, oY);
          }
        }
      },

      getDisplayId: function(node) {
        var nodeTemplateNameSizeCut = (this.isRuntime === true) ? 5 : 2;
        if (node.id.length >= UTILS.maxNodeNameDrawSize) {
          return node.id.substring(0, UTILS.maxNodeNameDrawSize - nodeTemplateNameSizeCut) + '...';
        } else {
          return node.id;
        }
      },

      drawRuntimeInfos: function(runtimeGroup, nodeInstances, nodeInstancesCount, rectOriginX, rectOriginY) {
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
          if (failureCount > 0) {
            this.drawRuntimeCount(runtimeGroup, 'runtime-count-failure', rectOriginX, currentY, '\uf00d', failureCount, nodeInstancesCount);
          } else {
            this.removeRuntimeCount(runtimeGroup, 'runtime-count-failure');
          }

          if (nodeInstancesCount === successCount) {
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 17).attr('cy', rectOriginY + 16).attr('r', '12')
              .attr('orient', 'auto').attr('style', 'stroke: none; fill: green');
          } else if(nodeInstancesCount === failureCount){
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 17).attr('cy', rectOriginY + 16).attr('r', '12')
            .attr('orient', 'auto').attr('style', 'stroke: none; fill: red');
          }else if(nodeInstancesCount === deletedCount ){
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 17).attr('cy', rectOriginY + 16).attr('r', '12').attr(
              'orient', 'auto').attr('style', 'stroke: none; fill: gray');
          }else{
            runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 17).attr('cy', rectOriginY + 16).attr('r', '12')
              .attr('orient', 'auto').attr('style', 'stroke: none; fill: orange');
          }
        } else {
          //unknown status
          this.drawRuntimeCount(runtimeGroup, 'runtime-count-unknown', rectOriginX, currentY, '\uf110');
          runtimeGroup.append('circle').attr('cx', rectOriginX + this.width - 17).attr('cy', rectOriginY + 16).attr('r', '12').attr(
            'orient', 'auto').attr('style', 'stroke: none; fill: gray');
        }

        runtimeGroup.append('text').attr('text-anchor', 'start').attr('x', rectOriginX + this.width - 20).attr('y', rectOriginY + 20).attr('font-weight',
          'bold').attr('fill', 'white').text(function() {
            return nodeInstancesCount ? nodeInstancesCount - deletedCount : 0;
          });
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
      },
      tooltip: function(node) {
        return node.id;
      }
    };
  } // function
]);
