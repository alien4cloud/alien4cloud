define(function (require) {
  'use strict';

  var modules = require('modules');
  var $ = require('jquery');
  var _ = require('lodash');

  require('scripts/tosca/services/tosca_service');

  require('scripts/common-graph/services/router_factory_service');
  require('scripts/common-graph/services/bbox_factory_service');
  require('scripts/topology/services/topology_tree_service');

  modules.get('a4c-topology-editor', ['ngResource']).factory('topologyLayoutService', ['toscaService', 'topologyTreeService', 'routerFactoryService', 'bboxFactory',
    function(toscaService, topologyTreeService, routerFactoryService, bboxFactory) {
      var xSpacing = 60;

      return {
        layout: function(nodeTemplates, topology, renderer) {
          var tree = topologyTreeService.buildTree(nodeTemplates, topology);
          topologyTreeService.sortTree(tree, topology);

          // process the layout of all tree elements but network
          this.treeLayout(tree, renderer);
          // compute graph
          var treeGraph = this.buildTreeGraph(tree);
          // manage networks
          this.networkLayout(tree, treeGraph, renderer);
          this.linkLayout(treeGraph, topology.relationshipTypes);
          return treeGraph;
        },

        buildTreeGraph: function(tree) {
          // Extract our graph with node and links
          var bbox = bboxFactory.create();
          var graph = {
            nodes : [],
            nodeMap: {},
            links : [],
            bbox : bbox,
            addNode: function(node) {
              this.nodes.push(node);
              this.nodeMap[node.id] = node;
            }
          };
          for (var i = 0; i < tree.children.length; i++) {
            this.addNodeToGraph(graph, tree.children[i]);
          }
          return graph;
        },

        addNodeToGraph: function(graph, node) {
          var i;
          // add the node to the graph
          graph.addNode(node);
          graph.bbox.add(node.bbox);

          for (i = 0; i < node.attached.length; i++) {
            this.addNodeToGraph(graph, node.attached[i]);
          }
          for (i = 0; i < node.children.length; i++) {
            this.addNodeToGraph(graph, node.children[i]);
          }
        },

        treeLayout: function(tree, renderer) {
          var position = {x: 0, y: 0};
          for (var i = 0; i < tree.children.length; i++) {
            this.nodeLayout(tree.children[i], position, renderer, true);
            // distance between the branches
            position.x += xSpacing + tree.children[i].bbox.width();
            position.y = 0;
          }
        },

        nodeLayout: function(node, position, renderer, initial) {
          node.nodeSize = renderer.size(node);
          node.nodeSize.halfWidth = node.nodeSize.width / 2;
          node.nodeSize.halfHeight = node.nodeSize.height / 2;
          node.bbox = bboxFactory.create(position.x, position.y, node.nodeSize.width, node.nodeSize.height);
          node.coordinate = {x: position.x, y: position.y};

          if(node.children.length > 0) {
            for(var i = 0; i < node.children.length; i++) {
              var childPosition = {
                x: node.bbox.minX + 5,
                y: node.bbox.maxY + 5
              };
              var childBbox = this.nodeLayout(node.children[i], childPosition, renderer, false);
              node.bbox.addPoint(childBbox.maxX + 5, childBbox.maxY);
            }
            node.bbox.addPoint(node.bbox.maxX, node.bbox.maxY + 5);
          }

          // attached node are quite similar but doesn't impact node's bbox as they are not contained
          var attachedY = node.bbox.maxY;
          for(var j = 0; j < node.attached.length; j++) {
            var attached = node.attached[j];
            attached.nodeSize = renderer.size(attached);
            attached.nodeSize.width = node.bbox.width(); // the attached node must have the node width (should not have child ?).
            attached.bbox = bboxFactory.create(node.bbox.minX, attachedY, attached.nodeSize.width, attached.nodeSize.height);
            attached.coordinate = {x: attached.bbox.minX, y: attached.bbox.minY};
            attachedY = attached.bbox.maxY;
          }

          return node.bbox;
        },

        networkLayout: function(tree, graph, renderer) {
          var maxWidth = 0;
          _.each(tree.networks, function(node) {
            node.nodeSize = renderer.size(node);
            maxWidth = Math.max(maxWidth, node.nodeSize.width);
          });
          var networkX = graph.bbox.minX - maxWidth - xSpacing;
          var netYSpacing = 40;
          for(var i=0; i<tree.networks.length; i++) {
            var node = tree.networks[i];
            node.coordinate = {
              x: networkX,
              y: graph.bbox.minY - node.nodeSize.height - netYSpacing
            };
            netYSpacing = 0;
            node.networkId = i;
            node.bbox = bboxFactory.create(node.coordinate.x , node.coordinate.y , node.nodeSize.width, node.nodeSize.height);
            graph.addNode(node);
            graph.bbox.addPoint(node.coordinate.x, node.coordinate.y);
          }
        },

        linkLayout: function(graph, relationshipTypes) {
          var self = this;
          _.each(graph.nodes, function(node) {
            // For each node map the relationships to the graph.
            self.buildGraphRelationships(graph, node, relationshipTypes);
          });
        },

        buildGraphRelationships: function(graph, node, relationshipTypes) {
          var networkCount = 0;
          var relationships = toscaService.getRelationships(node.template, function(relationship) {
            return toscaService.isOneOfType(['tosca.relationships.Root'], relationship.type, relationshipTypes);
          });
          if (_.defined(relationships)) {
            var relationshipsLength = relationships.length;
            for (var i = 0; i < relationshipsLength; i++) {
              var relationship = relationships[i];
              var targetNode = graph.nodeMap[relationship.target];
              var source = {};
              var target = {};
              var isNetwork = false;
              var networkId = null;
              if(toscaService.isAttachedToType(relationship.type, relationshipTypes) ||
                  toscaService.isHostedOnType(relationship.type, relationshipTypes)) {
                continue;
              }
              if(toscaService.isNetworkType(relationship.type, relationshipTypes)){
                isNetwork = true;
                networkCount++;
                source.x = node.coordinate.x + 14 * networkCount;
                source.y = node.coordinate.y;
                target.x = source.x;
                target.y = targetNode.coordinate.y + targetNode.bbox.height() / 2;
                networkId = graph.nodeMap[relationship.target].networkId;
              } else {
                source.x = node.coordinate.x + node.bbox.width() + 1;
                source.y = node.coordinate.y + node.nodeSize.halfHeight;
                target.x = targetNode.coordinate.x - 1;
                target.y = targetNode.coordinate.y + targetNode.nodeSize.halfHeight;
                source.direction = routerFactoryService.directions.right;
                target.direction = routerFactoryService.directions.left;
              }
              var selected = graph.nodeMap[relationship.target].template.selected || node.template.selected;
              graph.links.push({
                id: node.id + '.' + relationship.id,
                type: relationship.type,
                source: source,
                target: target,
                selected: selected,
                isNetwork: isNetwork,
                networkId: networkId
              });
            }
          }
        }
      };
    }
  ]); // factory
});// define
