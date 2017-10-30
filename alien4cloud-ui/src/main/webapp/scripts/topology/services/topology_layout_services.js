define(function (require) {
  'use strict';
  const X_META = 'a4c_edit_x';
  const Y_META = 'a4c_edit_y';

  var modules = require('modules');
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
              node.isRoot = false;
            }
          };
          for (var i = 0; i < tree.children.length; i++) {
            this.addNodeToGraph(graph, tree.children[i]);
            tree.children[i].isRoot = true;
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
            var node = tree.children[i];
            var nodePosition = position;
            if(_.defined(node.template.metadata[X_META])) {
              nodePosition = {
                x: parseInt(node.template.metadata[X_META]),
                y: parseInt(node.template.metadata[Y_META])
              };
            }

            this.nodeLayout(tree.children[i], nodePosition, renderer, true);
            // distance between the branches
            position.x += xSpacing + tree.children[i].bbox.width();
            position.y = 0;
          }
        },

        nodeLayout: function(node, position, renderer) {
          node.nodeSize = renderer.size(node);
          node.nodeSize.halfWidth = node.nodeSize.width / 2;
          node.nodeSize.halfHeight = node.nodeSize.height / 2;
          node.bbox = bboxFactory.create(position.x, position.y, node.nodeSize.width, node.nodeSize.height);

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
            attached.nodeSize.halfWidth = attached.nodeSize.width / 2;
            attached.nodeSize.halfHeight = attached.nodeSize.height / 2;
            attached.bbox = bboxFactory.create(node.bbox.minX, attachedY, attached.nodeSize.width, attached.nodeSize.height);
            attachedY = attached.bbox.maxY;

            // process the capabilites and requirements of the attached node as we don't perform recursive layout for these nodes
            this.requirementAndCapabilitiesLayout(attached);
          }
          this.requirementAndCapabilitiesLayout(node);

          return node.bbox;
        },

        /**
        * Update the requirements and capabilities relative x coordinate.
        */
        requirementAndCapabilitiesLayout: function(node) {
          _.each(node.requirements, function(requirement) {
            requirement.coordinate.relative.x = node.bbox.width();
            requirement.coordinate.x = node.bbox.x() + requirement.coordinate.relative.x;
            requirement.coordinate.y = node.bbox.y() + requirement.coordinate.relative.y;
          });
          _.each(node.capabilities, function(capability) {
            capability.coordinate.x = node.bbox.x() + capability.coordinate.relative.x;
            capability.coordinate.y = node.bbox.y() + capability.coordinate.relative.y;
          });
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
            var nodeCoordinate = {
              x: networkX,
              y: graph.bbox.minY - node.nodeSize.height - netYSpacing
            };
            netYSpacing = 0;
            node.networkId = i;
            node.bbox = bboxFactory.create(nodeCoordinate.x , nodeCoordinate.y , node.nodeSize.width, node.nodeSize.height);
            graph.addNode(node);
            graph.bbox.addPoint(node.bbox.x(), node.bbox.y());
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
              if(toscaService.isAttachedToType(relationship.type, relationshipTypes) ||
                  toscaService.isHostedOnType(relationship.type, relationshipTypes)) {
                continue;
              }
              var targetNode = graph.nodeMap[relationship.target];
              // if not found then take the node self connector.
              var source = {};
              var target = {};
              var isNetwork = false;
              var networkId = null;
              if(toscaService.isNetworkType(relationship.type, relationshipTypes)){
                isNetwork = true;
                networkCount++;
                source.x = node.bbox.x() + 14 * networkCount;
                source.y = node.bbox.y();
                target.x = source.x;
                target.y = targetNode.bbox.y() + targetNode.bbox.height() / 2;
                networkId = graph.nodeMap[relationship.target].networkId;
              } else {
                // find the target capabilities / requirements
                var sourceRequirement = node.requirementsMap[relationship.requirementName];
                source.x = node.bbox.x() + node.bbox.width() + 5;
                if(_.defined(sourceRequirement)) {
                  source.y = node.bbox.y() + sourceRequirement.coordinate.relative.y;
                } else {
                  source.y = node.bbox.y();
                }
                var targetCapability = targetNode.capabilitiesMap[relationship.targetedCapabilityName];
                target.x = targetNode.bbox.x() - 5;
                if(_.defined(targetCapability)) {
                  target.y = targetNode.bbox.y() + targetCapability.coordinate.relative.y;
                } else {
                  target.y = targetNode.bbox.y();
                }
                source.direction = routerFactoryService.directions.right;
                target.direction = routerFactoryService.directions.left;
              }
              graph.links.push({
                id: node.id + '.' + relationship.id,
                type: relationship.type,
                source: source,
                target: target,
                sourceTemplate: node.template,
                targetTemplate: graph.nodeMap[relationship.target].template,
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
