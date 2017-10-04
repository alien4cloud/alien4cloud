define(function(require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');
  const COMPONENT_VERSION = 'component_version';


  require('scripts/tosca/services/tosca_service');
  require('scripts/topology/services/common_node_renderer_service');
  require('scripts/common/services/list_to_map_service');

  modules.get('a4c-topology-editor', ['a4c-common', 'a4c-styles', 'a4c-common-graph']).factory('defaultNodeRendererService', ['commonNodeRendererService', 'toscaService', 'listToMapService', 'runtimeColorsService', 'd3Service',
    function(commonNodeRendererService, toscaService, listToMapService, runtimeColorsService, d3Service) {
      var ConnectorRenderer = function(isRequirement, isRuntime) {
        if (isRequirement) {
          this.cssClass = 'requirement';
        } else {
          this.cssClass = 'capability';
        }
        this.isRequirement = isRequirement;
        this.isRuntime = isRuntime;
      };

      ConnectorRenderer.prototype = {
        constructor: ConnectorRenderer,
        enter: function(enterSelection) {
          return enterSelection.append('g').attr('class', this.cssClass);
        },
        create: function(group, element) {
          d3Service.circle(group, element.coordinate.relative.x, element.coordinate.relative.y, 5).attr('class', 'connector');
          if (this.isRuntime) {
            return;
          }
          // actually create bigger circle for user interactions to make it easier.
          var actionCircle = d3Service.circle(group, element.coordinate.relative.x, element.coordinate.relative.y, 10).attr('class', 'connectorAction');
          actionCircle.on('mouseover', this.actions.mouseover).on('mouseout', this.actions.mouseout);
          if (this.isRequirement) {
            actionCircle.call(this.actions.connectorDrag);
          }
        },
        update: function(group, element) {
          // we have to update the location of the circle
          var circle = group.select('.connector');
          circle.attr('cx', element.coordinate.relative.x);
          circle.attr('cy', element.coordinate.relative.y);
          if (this.isRuntime) {
            return;
          }
          // we have to update the drag behavior to work with the new selection element (if not it will keep the creation element).
          var actionCircle = group.select('.connectorAction');
          if (this.isRequirement) {
            actionCircle.call(this.actions.connectorDrag);
          }
          actionCircle.attr('cx', element.coordinate.relative.x);
          actionCircle.attr('cy', element.coordinate.relative.y);
        }
      };

      return {
        isRuntime: false,
        networkStyles: 10,

        setRuntime: function(isRuntime) {
          this.isRuntime = isRuntime;
          this.height = 50;
          this.requirementRenderer = new ConnectorRenderer(true);
          this.capabilityRenderer = new ConnectorRenderer(false);
        },

        size: function(node) {
          var connectorCount = Math.max(node.capabilities.length, node.requirements.length);
          var connectorHeight = connectorCount * 10 + (connectorCount + 1) * 5;
          var height = Math.max(50, connectorHeight);
          // inject the relative coordinates of the connectors
          this.placeConnectors(node.capabilities);
          this.placeConnectors(node.requirements);
          return {
            width: 200,
            height: height
          };
        },

        placeConnectors: function(connectors) {
          var relativeY = 10;
          _.each(connectors, function(connector) {
            connector.coordinate = {
              relative: {
                x: 0,
                y: relativeY
              }
            };
            relativeY += 15;
          });
        },

        createNode: function(layout, nodeGroup, node, nodeTemplate, nodeType, topology, actions) {
          d3Service.rect(nodeGroup, 0, 0, node.bbox.width(), node.bbox.height(), 0, 0).attr('class', 'background');
          // specific to the runtime view
          if (this.isRuntime) {
            var runtimeGroup = nodeGroup.append('g').attr('id', 'runtime');
            d3Service.rect(runtimeGroup, node.bbox.width() - 10, 0, 10, this.height, 0, 0).attr('id', 'runtimeState').attr('class', 'runtime-state-no');
          }
          if (nodeType.tags) {
            var tags = listToMapService.listToMap(nodeType.tags, 'name', 'value');
            if (tags.icon) {
              nodeGroup.append('image').attr('x', 8).attr('y', 8).attr('width', '32').attr('height', '32').attr('xlink:href',
                'img?id=' + tags.icon + '&quality=QUALITY_64');
            }
          }
          if (nodeType.abstract) {
            nodeGroup.append('image').attr('x', 44).attr('y', 26).attr('width', 16).attr('height', 16).attr('xlink:href', 'images/abstract_ico.png');
          }

          nodeGroup.append('text').attr('id', 'title_' + node.id).attr('text-anchor', 'start').attr('class', 'title').attr('x', 44).attr('y', 20);
          nodeGroup.append('text').attr('text-anchor', 'end').attr('class', 'version').attr('x', node.bbox.width() - 10).attr('y', 40);

          // if the node has some children we should add the collapse bar.
          // if(_.defined(node.children) && node.children.length>0) {
          //   d3Service.rect(nodeGroup, .5, 45, node.bbox.width()-1, 10, 0, 0).attr('class', 'collapsebar');
          // }

          // specific to networks
          if (toscaService.isOneOfType(['tosca.nodes.Network'], nodeTemplate.type, topology.nodeTypes)) {
            var netX = node.bbox.width();
            var netMaxX = layout.bbox.width();
            var netY = node.bbox.height() / 2 - 2;
            var netStyle = node.networkId % this.networkStyles;
            var path = 'M ' + netX + ',' + netY + ' ' + netMaxX + ',' + netY;
            nodeGroup.append('path').attr('d', path).attr('class', 'link-network link-network-' + netStyle + ' link-selected');
          }
          d3Service.rect(nodeGroup, 0, 0, node.bbox.width(), node.bbox.height(), 0, 0).attr('class', 'selector').attr('node-template-id', node.id)
            .attr('id', 'rect_' + node.id).on('click', actions.click).on('mouseover', actions.mouseover).on('mouseout', actions.mouseout);

          this.requirementRenderer.actions = actions;
          this.capabilityRenderer.actions = actions;
          d3Service.select(nodeGroup, node.requirements, '.requirement', this.requirementRenderer);
          d3Service.select(nodeGroup, node.capabilities, '.capability', this.capabilityRenderer);
        },

        updateNode: function(nodeGroup, node, nodeTemplate, nodeType, topology) {
          nodeGroup.select('.background').attr('width', node.bbox.width()).attr('height', node.bbox.height());
          nodeGroup.select('.selector').attr('width', node.bbox.width()).attr('height', node.bbox.height());
          // update text
          nodeGroup.select('.title').text(commonNodeRendererService.getDisplayId(node, false));

          // update version
          nodeGroup.select('.version').text(function() {
            if (_.defined(_.get(nodeTemplate, 'propertiesMap.component_version.value.value'))) {
              return 'v' + nodeTemplate.propertiesMap[COMPONENT_VERSION].value.value;
            } else if(_.defined(_.get(nodeTemplate, 'propertiesMap.version.value.value'))) {
              return 'v' + nodeTemplate.propertiesMap.version.value.value;
            }
          });

          if (_.defined(nodeTemplate.groups)) {
            var gIdx = 1;
            // group square width is calculated using the node width (1/15)
            var gW = node.bbox.width() / 15;
            if (nodeTemplate.groups.length > 10) {
              // reduce the size of bullets when too many groups
              gW = node.bbox.width() / (nodeTemplate.groups.length + 10);
            }
            // group square height is calculated using the node height (about 1/2)
            var gH = 15;
            // the group y is near the 2/3 of the height of the node
            var gY = node.bbox.height() - 5;
            // the end of the node square
            var nodeEndX = node.bbox.width() - (gW / 2);
            angular.forEach(nodeTemplate.groups, function(value) {
              // let's place the group square regarding it's index and applying a 0.2 margin
              var gX = nodeEndX - (gIdx * 1.2 * gW);

              var rect = d3Service.rect(nodeGroup, gX, gY, gW, gH, 3, 3).attr('class', 'node-template-group ' + runtimeColorsService.groupColorCss(topology.topology, value));
              // add the group name as title (for popping over)
              rect.attr('title', value);
              gIdx++;
            });
          }

          // runtime infos
          if (this.isRuntime) {
            var runtimeGroup = nodeGroup.select('#runtime');
            var nodeInstances = null;
            var nodeInstancesCount = null;

            if (_.defined(topology.instances)) {
              nodeInstances = topology.instances[node.id];
              if (_.defined(nodeInstances)) {
                nodeInstancesCount = Object.keys(nodeInstances).length;
              }
            }

            // TODO better draw network node
            if (!toscaService.isOneOfType(['tosca.nodes.Network'], nodeTemplate.type, topology.nodeTypes)) {
              this.drawRuntimeInfos(node, runtimeGroup, nodeInstances, nodeInstancesCount, 0, 0);
            }
          } else { // scaling policy
            var scalingPolicy = toscaService.getScalingPolicy(nodeTemplate);
            var scalingPolicySelection = nodeGroup.select('#scalingPolicy');
            if (_.defined(scalingPolicy)) {
              var formatScalingValue = function(scalingValue) {
                if (scalingValue.hasOwnProperty('function')) {
                  return '...';
                } else {
                  return scalingValue;
                }
              };
              var scalingText = formatScalingValue(scalingPolicy.minInstances) + ' - ' + formatScalingValue(scalingPolicy.initialInstances) + ' - ' + formatScalingValue(scalingPolicy.maxInstances);
              if (scalingPolicySelection.empty()) {
                var scaleX = 50;
                if (nodeType.abstract) {
                  scaleX += 20;
                }
                scalingPolicySelection = nodeGroup.append('g').attr('id', 'scalingPolicy');
                scalingPolicySelection.append('text').attr('class', 'topology-svg-icon topology-svg-icon-center')
                  .attr('transform', 'rotate(90 ' + (scaleX) + ' 35)')
                  .attr('x', scaleX).attr('y', 35).text('\uf112');
                scalingPolicySelection.append('text').attr('id', 'scaling-text').attr('text-anchor', 'start')
                  .attr('x', scaleX + 10).attr('y', 40).text(scalingText);
              } else {
                scalingPolicySelection.select('#scaling-text').text(scalingText);
              }
            } else if (!scalingPolicySelection.empty()) {
              scalingPolicySelection.remove();
            }
          }

          d3Service.select(nodeGroup, node.requirements, '.requirement', this.requirementRenderer);
          d3Service.select(nodeGroup, node.capabilities, '.capability', this.capabilityRenderer);
        },

        drawRuntimeInfos: function(node, runtimeGroup, nodeInstances, nodeInstancesCount) {
          var deletedCount = 0;
          var indicatorX = 44;

          var runtimeStateSelection = runtimeGroup.select('#runtimeState');
          if (_.defined(nodeInstances) && nodeInstancesCount > 0) {
            //the deployment status is no more unknown
            this.removeRuntimeCount(runtimeGroup, 'runtime-count-unknown');

            var successCount = this.getNumberOfInstanceByStatus(nodeInstances, 'SUCCESS');
            var processingCount = this.getNumberOfInstanceByStatus(nodeInstances, 'PROCESSING');
            var maintenanceCount = this.getNumberOfInstanceByStatus(nodeInstances, 'MAINTENANCE');
            var failureCount = this.getNumberOfInstanceByStatus(nodeInstances, 'FAILURE');
            deletedCount = this.getNumberOfInstanceByStatus(nodeInstances, null, 'stopped');

            var runtimeStateIndicatorWidth = node.bbox.width() - 48 - 40;
            var indicatorWidth;
            if (successCount > 0) {
              indicatorWidth = runtimeStateIndicatorWidth * successCount / nodeInstancesCount;
              this.drawRuntimeCount(runtimeGroup, 'runtime-count-success', indicatorX, indicatorWidth, 'runtime-state-deployed');
              indicatorX += indicatorWidth;
            } else {
              this.removeRuntimeCount(runtimeGroup, 'runtime-count-success');
            }
            if (processingCount > 0) {
              indicatorWidth = runtimeStateIndicatorWidth * processingCount / nodeInstancesCount;
              this.drawRuntimeCount(runtimeGroup, 'runtime-count-progress', indicatorX, indicatorWidth, 'runtime-state-warning');
              indicatorX += indicatorWidth;
              // '\uf110'
            } else {
              this.removeRuntimeCount(runtimeGroup, 'runtime-count-progress');
            }
            if (maintenanceCount > 0) {
              indicatorWidth = runtimeStateIndicatorWidth * maintenanceCount / nodeInstancesCount;
              this.drawRuntimeCount(runtimeGroup, 'runtime-count-maintenance', indicatorX, indicatorWidth, 'runtime-state-maintenance');
              indicatorX += indicatorWidth;
              // '\uf0ad'
            } else {
              this.removeRuntimeCount(runtimeGroup, 'runtime-count-maintenance');
            }
            if (failureCount > 0) {
              indicatorWidth = runtimeStateIndicatorWidth * failureCount / nodeInstancesCount;
              this.drawRuntimeCount(runtimeGroup, 'runtime-count-failure', indicatorX, indicatorWidth, 'runtime-state-failure');
              indicatorX += indicatorWidth;
            } else {
              this.removeRuntimeCount(runtimeGroup, 'runtime-count-failure');
            }

            if (nodeInstancesCount === successCount) {
              runtimeStateSelection.attr('class', 'runtime-state-deployed');
            } else if (nodeInstancesCount === failureCount) {
              runtimeStateSelection.attr('class', 'runtime-state-failure');
            } else if (nodeInstancesCount === maintenanceCount) {
              runtimeStateSelection.attr('class', 'runtime-state-maintenance');
            } else if (nodeInstancesCount === deletedCount) {
              runtimeStateSelection.attr('class', 'runtime-state-undeployed');
            } else {
              runtimeStateSelection.attr('class', 'runtime-state-warning');
            }
          } else { //unknown status
            runtimeStateSelection.attr('class', 'runtime-state-unknown');
            // this.drawRuntimeCount(runtimeGroup, 'runtime-count-unknown', indicatorX, currentY, '\uf110');
          }
        },

        drawRuntimeCount: function(runtimeGroup, id, x, width, indicatorClass) {
          var groupSelection = runtimeGroup.select('#' + id);
          if (groupSelection.empty()) {
            // iconCode ?
            d3Service.rect(runtimeGroup, x, 26, width, 4, 0, 0)
              .attr('id', id)
              .attr('class', indicatorClass);
          } else {
            groupSelection.attr('width', width);
          }
          // var groupSelection = runtimeGroup.select('#' + id);
          // // improve that...
          // var counter = (count || '?') + '/' + (instanceCount || '?');
          // if (groupSelection.empty()) {
          //   groupSelection = runtimeGroup.append('g').attr('id', id);
          //   groupSelection.append('text').attr('class', 'topology-svg-icon').attr('text-anchor', 'start').attr('x', rectOriginX + 60).attr('y', currentY).text(iconCode);
          //   groupSelection.append('text').attr('id', 'count-text').attr('text-anchor', 'start').attr('x', rectOriginX + 80).attr('y', currentY).text(counter);
          // } else {
          //   groupSelection.select('#count-text').text(counter);
          // }
        },

        // common services
        removeRuntimeCount: commonNodeRendererService.removeRuntimeCount,
        getNumberOfInstanceByStatus: commonNodeRendererService.getNumberOfInstanceByStatus,
        tooltip: commonNodeRendererService.tooltip
      };
    } // function
  ]); // factory
}); // define
