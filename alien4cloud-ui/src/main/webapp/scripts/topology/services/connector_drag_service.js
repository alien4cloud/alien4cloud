/* global d3 */
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var d3Tip = require('d3-tip');

  modules.get('a4c-topology-editor').factory('connectorDragFactoryService', [ 'relationshipMatchingService',
    function(relationshipMatchingService) {
      return {
        create: function(topologySvg) {
          var selectedTarget;
          var mouseCoordinate;
          var tip = d3Tip().attr('class', 'd3-tip').html(function (node) {
            return node.target.id;
          });

          var connectorDrag = d3.behavior.drag()
            .on('dragstart', function(element) {
              // reinit the target selection
              selectedTarget = null;
              relationshipMatchingService.getTargets(element.node.id, element.template, element.id, topologySvg.topology.topology.nodeTemplates,
                topologySvg.topology.nodeTypes, topologySvg.topology.relationshipTypes, topologySvg.topology.capabilityTypes, topologySvg.topology.topology.dependencies).then(function(result) {
                var connectTargets = [];
                // TODO if drag & drop is still active
                _.each(result.targets, function(target) {
                  var targetNode = topologySvg.layout.nodeMap[target.template.name];
                  _.each(target.capabilities, function(targetCapabilityInfo){
                    var targetCapability = targetNode.capabilitiesMap[targetCapabilityInfo.id];
                    if(_.defined(targetCapability)) {
                      // add a drop target
                      connectTargets.push({
                        id: targetNode.id + '.' + targetCapability.id,
                        target: targetCapability,
                        relationship: result.relationshipType
                      });
                    }
                  });
                });


                var targetSelection = topologySvg.svg.selectAll('.connectorTarget').data(connectTargets);
                var vis = targetSelection.enter().append('circle')
                  .attr('cx', function(d){ return d.target.coordinate.x; })
                  .attr('cy', function(d){ return d.target.coordinate.y; })
                  .attr('r', 10)
                  .attr('class', 'connectorTarget')
                  .attr('pointer-events', 'mouseover')
                  .on('mouseover', function(node) { selectedTarget = node; tip.show(node);})
                  .on('mouseout', function() { selectedTarget = null; tip.hide(); });
                vis.call(tip);
                targetSelection.exit().remove();
              });
              mouseCoordinate = {
                x: element.coordinate.x,
                y: element.coordinate.y
              };

              // find relationship valid targets
              d3.event.sourceEvent.stopPropagation();
            }).on('drag', function(element) {
              var data = [];
              mouseCoordinate.x += d3.event.dx;
              mouseCoordinate.y += d3.event.dy;
              data = [{
                source: {
                  x: element.coordinate.x,
                  y: element.coordinate.y
                },
                target: mouseCoordinate
              }];
              var link = topologySvg.svg.selectAll('.connectorlink').data(data);
              link.enter().append('path')
                  .attr('class', 'connectorlink')
                  .attr('d', d3.svg.diagonal())
                  .attr('pointer-events', 'none');
              link.attr('d', d3.svg.diagonal());
              link.exit().remove();
            }).on('dragend', function(element) {
              // remove all drag line and drag targets
              topologySvg.svg.selectAll('.connectorTarget').data([]).exit().remove();
              topologySvg.svg.selectAll('.connectorlink').data([]).exit().remove();
              tip.hide();

              if(_.defined(selectedTarget)) {
                var target = selectedTarget.target;
                topologySvg.callbacks.addRelationship({
                  sourceId: element.node.id,
                  requirementName: element.id,
                  requirementType: element.template.type,
                  targetId: target.node.id,
                  capabilityName: target.id,
                  relationship: selectedTarget.relationship
                });
              }
            });

          return connectorDrag;
        } // create
      }; // return
    } // function
  ]); // factory
}); // define
