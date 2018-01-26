/* global d3 */
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('nodeDragFactoryService', [ 'topologyLayoutService',
    function(topologyLayoutService) {
      return {
        create: function(topologySvgService) {
          var dragInfo, self = this;
          var nodeDrag = d3.behavior.drag()
            .origin(function(d) {
              dragInfo = {
                node: d,
                children: self.getAllChildren(d)
              };
              return { x: d.bbox.x(), y: d.bbox.y() };
            })
            .on('dragstart', function() {
              d3.event.sourceEvent.stopPropagation();
            })
            .on('drag', function() {
              if(_.undefined(topologySvgService, 'callbacks.updateNodePosition')) {
                return;
              }
              if(_.undefined(dragInfo.node.parent) || _.defined(dragInfo.node.parent.id)) {
                // All nodes but network have the graph has parent, graph has no id property.
                return; // If a node is a network or child node we don't move it.
              }
              d3.select(this).attr('transform', function() {
                return 'translate(' + d3.event.x + ',' + d3.event.y + ')';
              });
              // Update children nodes coordinates
              topologyLayoutService.nodeLayout(dragInfo.node, {x: d3.event.x, y: d3.event.y}, topologySvgService.nodeRenderer);
              // Update location of children nodes
              var nodeSelection = topologySvgService.svg.selectAll('.node-template').data(dragInfo.children, function(node) { return node.id; });
              nodeSelection.each(function(node) {
                var nodeGroup = d3.select(this);
                nodeGroup.attr('transform', function() {
                  return 'translate(' + node.bbox.x() + ',' + node.bbox.y() + ')';
                });
              });
              // TODO update links using basic coordinates

              dragInfo.position = { x: d3.event.x, y: d3.event.y };
            })
            .on('dragend', function() {
              if(_.defined(dragInfo, 'position')) {
                topologySvgService.callbacks.updateNodePosition(dragInfo.node.id, dragInfo.position.x, dragInfo.position.y);
              }
              dragInfo = undefined;
            });

          return nodeDrag;
        }, // create
        /**
        * Get all children (including sub-children) of the given node to the children array.
        */
        getAllChildren: function(node, children) {
          if(_.undefined(node.children)) {
            return;
          }
          if(_.undefined(children)) {
            children = [];
          }
          for(var i=0; i < node.children.length; i++) {
            children.push(node.children[i]);
            this.getAllChildren(node.children[i], children);
          }
          return children;
        }
      }; // return
    } // function
  ]); // factory
}); // define
