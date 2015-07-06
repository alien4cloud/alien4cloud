define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/tosca/services/tosca_service');
  require('scripts/topology/services/common_node_renderer_service');
  require('scripts/common/services/list_to_map_service');

  modules.get('a4c-topology-editor', ['a4c-common', 'a4c-styles', 'a4c-common-graph']).factory('defaultNodeRendererService', ['commonNodeRendererService', 'toscaService', 'listToMapService', 'runtimeColorsService', 'd3Service',
    function(commonNodeRendererService, toscaService, listToMapService, runtimeColorsService, d3Service) {
      var ConnectorRenderer = function(cssClass) {
        this.cssClass = cssClass;
      };

      ConnectorRenderer.prototype = {
        constructor: ConnectorRenderer,
        enter: function(enterSelection) {
          return enterSelection.append('g').attr('class', this.cssClass);
        },
        create: function(group, element) {
          d3Service.circle(group, element.coordinate.relative.x, element.coordinate.relative.y, 5).attr('class', 'connector');
          // actually create bigger circle for user interactions to make it easier.
          var actionCircle = d3Service.circle(group, element.coordinate.relative.x, element.coordinate.relative.y, 10).attr('class', 'connectorAction');
          actionCircle.on('mouseover', this.actions.mouseover).on('mouseout', this.actions.mouseout);
          actionCircle.call(this.actions.connectorDrag);
        },
        update: function(group, element) {
          // we have to update the drag behavior to work with the new selection element (if not it will keep the creation element).
          var actionCircle = group.select('.connectorAction');
          actionCircle.call(this.actions.connectorDrag);
        }
      };

      var requirementRenderer = new ConnectorRenderer('requirement');
      var capabilityRenderer = new ConnectorRenderer('capability');

      return {
        isRuntime: false,
        networkStyles: 10,

        setRuntime: function(isRuntime) {
          this.isRuntime = isRuntime;
          if (isRuntime) {
            this.height = 85;
          } else {
            this.height = 50;
          }
        },

        size: function(node) {
          var connectorCount = Math.max(node.capabilities.length, node.requirements.length);
          var connectorHeight = connectorCount * 10 + (connectorCount+1) * 5;
          var height = Math.max(50, connectorHeight);
          // inject the relative coordinates of the connectors
          this.placeConnectors(node.capabilities);
          this.placeConnectors(node.requirements);
          return {
            width: 200,
            height: height
          }
        },

        placeConnectors: function(connectors) {
          var relativeY = 10;
          _.each(connectors, function(connector) {
            connector.coordinate = {
              relative: {
                x: 0,
                y: relativeY
              }
            }
            relativeY += 15;
          });
        },

        createNode: function(layout, nodeGroup, node, nodeTemplate, nodeType, topology, actions) {
          var backRect = d3Service.rect(nodeGroup, 0, 0, node.bbox.width(), node.bbox.height(), 0, 0).attr('class', 'background');

          if (nodeType.tags) {
            var tags = listToMapService.listToMap(nodeType.tags, 'name', 'value');
            if (tags.icon) {
              nodeGroup.append('image').attr('x', 8).attr('y', 8).attr('width', '32').attr('height', '32').attr('xlink:href',
                'img?id=' + tags.icon + '&quality=QUALITY_32');
            }
          }

          if (nodeType.abstract) {
            var icoSize = 16;
            var x = 180;
            var y = 3;
            nodeGroup.append('image').attr('x', x).attr('y', y).attr('width', icoSize).attr('height', icoSize).attr('xlink:href', 'images/abstract_ico.png');
          }

          nodeGroup.append('text').attr('text-anchor', 'start').attr('class', 'title').attr('x', 44).attr('y', 20);
          nodeGroup.append('text').attr('text-anchor', 'end').attr('class', 'version').attr('x', '80').attr('y', '20');

          // specific to the runtime view
          if (this.isRuntime) {
            nodeGroup.append('g').attr('id', 'runtime');
          }

          // specific to networks
          if (toscaService.isOneOfType(['tosca.nodes.Network'], nodeTemplate.type, topology.nodeTypes)) {
            var netX = node.bbox.width();
            var netMaxX = layout.bbox.width();
            var netY = node.bbox.height()/2 - 2;
            var netStyle = node.networkId % this.networkStyles;
            var path = 'M '+netX+','+netY+' '+netMaxX+','+netY;
            nodeGroup.append('path').attr('d', path).attr('class', 'link-network link-network-' + netStyle + ' link-selected');
          }
          d3Service.rect(nodeGroup, 0, 0, node.bbox.width(), node.bbox.height(), 0, 0).attr('class', 'selector').attr('node-template-id', node.id)
            .attr('id', 'rect_' + node.id).on('click', actions.click).on('mouseover', actions.mouseover).on('mouseout', actions.mouseout);

          requirementRenderer.actions= actions;
          capabilityRenderer.actions= actions;
          d3Service.select(nodeGroup, node.requirements, '.requirement', requirementRenderer);
          d3Service.select(nodeGroup, node.capabilities, '.capability', capabilityRenderer);
        },

        updateNode: function(nodeGroup, node, nodeTemplate, nodeType, topology) {
          nodeGroup.select('.background').attr('width', node.bbox.width()).attr('height', node.bbox.height());
          nodeGroup.select('.selector').attr('width', node.bbox.width()).attr('height', node.bbox.height());
          // update text
          nodeGroup.select('.title').text(commonNodeRendererService.getDisplayId(node, false));

          // update version
          nodeGroup.select('.version').text(function() {
            if (_.defined(nodeTemplate.properties) && _.defined(nodeTemplate.properties.version)) {
              return 'v' + nodeTemplate.properties.version.value;
            }
          });

          // runtime infos
          if (this.isRuntime) {
            var runtimeGroup = nodeGroup.select('#runtime');

            var nodeInstances = null;
            var nodeInstancesCount = null;
            var nodeScalingPolicies = null;

            if (_.defined(topology.instances)) {
              nodeInstances = topology.instances[node.id];
              if (_.defined(nodeInstances)) {
                nodeInstancesCount = Object.keys(nodeInstances).length;
                nodeScalingPolicies = toscaService.getScalingPolicy(nodeTemplate);
              }
            }

            // TODO better draw network node
            if (!toscaService.isOneOfType(['tosca.nodes.Network'], nodeTemplate.type, topology.nodeTypes)) {
              this.drawRuntimeInfos(runtimeGroup, nodeInstances, nodeInstancesCount, 0, 0, nodeScalingPolicies);
            }
          }

          if(_.defined(nodeTemplate.groups)) {
            var gIdx = 1;
            // group square width is calculated using the node width (1/15)
            var gW = node.bbox.width() / 15;
            if (nodeTemplate.groups.length > 10) {
              // reduce the size of bullets when too many groups
              gW = node.bbox.width() / (nodeTemplate.groups.length + 10);
            }
            // group square height is calculated using the node height (about 1/2)
            var gH = node.bbox.height() / 2.5;
            // the group y is near the 2/3 of the height of the node
            var gY = 2.2 * node.bbox.height() / 3;
            // the end of the node square
            var nodeEndX = node.bbox.width() - (gW / 2);
            angular.forEach(nodeTemplate.groups, function(value, key) {
              // let's place the group square regarding it's index and applying a 0.2 margin
              var gX = nodeEndX - (gIdx * 1.2 * gW);

              var rect = d3Service.rect(nodeGroup, gX, gY, gW, gH, 3, 3).attr('class', 'node-template-group ' + runtimeColorsService.groupColorCss(topology.topology, value));
              // add the group name as title (for popping over)
              rect.attr('title', value);

              gIdx++;
            });
          }

          d3Service.select(nodeGroup, node.requirements, '.requirement', requirementRenderer);
          d3Service.select(nodeGroup, node.capabilities, '.capability', capabilityRenderer);
        },

        drawRuntimeInfos: function(runtimeGroup, nodeInstances, nodeInstancesCount, rectOriginX, rectOriginY, scalingPolicies) {
          var currentY = rectOriginY + 40;
          var deletedCount = 0;
          if (_.defined(nodeInstances) && nodeInstancesCount > 0) {
            //the deployment status is no more unknown
            this.removeRuntimeCount(runtimeGroup, 'runtime-count-unknown');

            var successCount = this.getNumberOfInstanceByStatus(nodeInstances, 'SUCCESS');
            var processingCount = this.getNumberOfInstanceByStatus(nodeInstances, 'PROCESSING');
            var maintenanceCount = this.getNumberOfInstanceByStatus(nodeInstances, 'MAINTENANCE');
            var failureCount = this.getNumberOfInstanceByStatus(nodeInstances, 'FAILURE');
            deletedCount = this.getNumberOfInstanceByStatus(nodeInstances, null, 'stopped');

            // adapt instance count
            if (_.defined(scalingPolicies)) {
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
  ]); // factory
}); // define
