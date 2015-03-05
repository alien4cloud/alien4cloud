/* global UTILS, $ */
'use strict';

angular.module('alienUiApp').factory(
    'topologyLayoutService', ['toscaService','routerFactoryService',
    function(toscaService, routerFactoryService) {
      return {
        layout: function(nodeTemplates, nodeTypes, relationshipTypes, nodeSize, spacing) {
          nodeSize.halfWidth = nodeSize.width / 2;
          nodeSize.halfHeight = nodeSize.height / 2;

          var nodeTemplatesCopy = UTILS.deepCopy(nodeTemplates);
          var tree = this.buildTree(nodeTemplatesCopy, nodeTypes, relationshipTypes);
          this.sortTree(tree, relationshipTypes, nodeTemplatesCopy);
          // process the layout of all tree elements but network
          this.treeLayout(tree, 0, nodeSize, spacing);
          // compute graph
          var treeGraph = this.buildTreeGraph(tree, nodeTemplatesCopy, relationshipTypes, nodeSize);
          // manage networks
          this.networkLayout(tree, treeGraph, nodeSize, spacing);
          this.linkLayout(treeGraph, nodeTemplatesCopy, nodeTypes, relationshipTypes, nodeSize, spacing);
          return treeGraph;
        },

        /**
        * Build a tree of nodes based on tosca relationships.
        *
        * @param nodeTemplates The map of node templates in the topology.
        * @param relationshipTypes Map of relationship types used in the topology.
        */
        buildTree: function(nodeTemplates, nodeTypes, relationshipTypes) {
          var thiss = this;
          // define the root of the tree that will be returned
          var tree = {
            'children' : [],
            'networks': []
          };
          if(!nodeTemplates) {
            return tree;
          }
          $.each(nodeTemplates, function(nodeId, nodeTemplate) {
            nodeTemplate.name = nodeId;
            // network are not managed like other nodes
            if(toscaService.isOneOfType(['tosca.nodes.Network'], nodeTemplate.type, nodeTypes)) {
              tree.networks.push(nodeTemplate);
            } else {
              thiss.addNodeTemplateToTree(tree, nodeTemplate, nodeTemplates, relationshipTypes);
            }
          });

          var visitedNodes = {};
          for(var i=0;i<tree.children.length;i++) {
            this.computeNodeDepth(tree.children[i], visitedNodes, 0);
          }

          return tree;
        },

        addNodeTemplateToTree: function(tree, nodeTemplate, nodeTemplates, relationshipTypes) {
          if(UTILS.isUndefinedOrNull(nodeTemplate.children)) {
            nodeTemplate.children = [];
          }
          if(UTILS.isUndefinedOrNull(nodeTemplate.attached)) {
            nodeTemplate.attached = [];
          }
          nodeTemplate.weight = 0;
          // get relationships that we want to display as hosted on.
          var relationships = this.getHostedOnRelationships(nodeTemplate, relationshipTypes);
          if (relationships.length > 0) {
            // TODO we should not have more than a single hosted on actually. Manage if not.
            var parent = nodeTemplates[relationships[0].target];
            UTILS.safePush(parent, 'children', nodeTemplate);
            nodeTemplate.parent = parent;
          } else {
            // Manage the attach relationship in a specific way in order to display the storage close to the compute.
            relationships = this.getAttachedToRelationships(nodeTemplate, relationshipTypes);
            if (relationships.length > 0) {
              var target = nodeTemplates[relationships[0].target];
              UTILS.safePush(target, 'attached', nodeTemplate);
              nodeTemplate.parent = tree;
              nodeTemplate.isAttached = true;
            } else {
              // if the node is not hosted on another node just add it to the root.
              tree.children.push(nodeTemplate);
              nodeTemplate.parent = tree;
            }
          }
        },

        /**
        * Quickly go over the tree to add a depth field to every node (this will be used for sorting later on).
        */
        computeNodeDepth: function(node, visitedNodes, parentDepth) {
          if (node.name in visitedNodes) {
            // Do not visit an already visited node to prevent cyclic dependencies
            console.log('Hosted on cyclic dependency detected, topology may not be valid.');
            return;
          }
          visitedNodes[node.name] = node;
          node.depth = parentDepth + 1;
          $.each(node.attached, function(index, value){
            value.depth = node.depth;
          });
          for (var i = 0; i < node.children.length; i++) {
            this.computeNodeDepth(node.children[i], visitedNodes, node.depth);
          }
        },

        /**
        * Get all hosted on or network relationships on a given node template.
        */
        getHostedOnRelationships: function(nodeTemplate, relationshipTypes) {
          var hostedOnRelationships = toscaService.getRelationships(nodeTemplate, function(relationship) {
            return toscaService.isHostedOnType(relationship.type, relationshipTypes);
          });
          return hostedOnRelationships;
        },

        /**
        * Get all attached to relationships on a given node template.
        */
        getAttachedToRelationships: function(nodeTemplate, relationshipTypes) {
          var relationships = toscaService.getRelationships(nodeTemplate, function(relationship) {
            return toscaService.isAttachedToType(relationship.type, relationshipTypes);
          });
          return relationships;
        },

        /**
        * Get all attached to relationships on a given node template.
        */
        getNetworkRelationships: function(nodeTemplate, relationshipTypes) {
          var relationships = toscaService.getRelationships(nodeTemplate, function(relationship) {
            return toscaService.isNetworkType(relationship.type, relationshipTypes);
          });
          return relationships;
        },

        /**
        * Sort the tree based on depends on relationships that connects nodes to each others.
        *
        * Each group from the root is displayed horizontally while children are displayed vertically.
        */
        sortTree: function(tree, relationshipTypes, nodeTemplates) {
          var thiss = this;
          // compute nodes weight and attractions.
          $.each(tree.children, function(index, child) {
            thiss.nodeWeight(tree, relationshipTypes, nodeTemplates, child);
          });
          this.recursiveTreeSort(tree);
        },
        recursiveTreeSort: function (node) {
          var children = [];
          for(var i = 0; i < node.children.length; i++) {
            var child = node.children[i];
            if(child.children.length >0) {
              this.recursiveTreeSort(child);
            }

            var candidatePosition = -1;
            if(UTILS.isDefinedAndNotNull(child.before)) { // ensure the node is added before.
              for(var j=0; j<children.length && candidatePosition === -1; j++) {
                if(child.before.indexOf(children[j]) >= 0) {
                  candidatePosition = j;
                }
              }
            }
            if(UTILS.isDefinedAndNotNull(child.after)) { // ensure the node is added after.
              for(var k=children.length-1; k>candidatePosition; k--) {
                if(child.after.indexOf(children[k]) >= 0) {
                  candidatePosition = k+1;
                }
              }
            }
            if(candidatePosition === -1) {
              children.push(child);
            } else {
              children.splice(candidatePosition, 0, child);
            }
          }
          node.children = children;
        },
        nodeWeight: function (tree, relationshipTypes, nodeTemplates, node) {
          var thiss = this;
          // process child nodes
          $.each(node.children, function(index, child){
            thiss.nodeWeight(tree, relationshipTypes, nodeTemplates, child);
          });
          // process current node
          var relationships = this.getDependsOnRelationships(node, relationshipTypes);
          $.each(relationships, function(index, relationship){
            var commonParentInfo = thiss.getCommonParentInfo(tree, node, nodeTemplates[relationship.target]);
            if(commonParentInfo !== null) {
              commonParentInfo.child2.weight += 10000;
              UTILS.safePush(commonParentInfo.child2, 'after', commonParentInfo.child1);
              UTILS.safePush(commonParentInfo.child1, 'before', commonParentInfo.child2);
            }
          });
        },
        getCommonParentInfo: function(tree, node1, node2) {
          // if both nodes doesn't have the same depth it cannot have the same parent.
          if(node1.depth < node2.depth) {
            return this.getCommonParentInfo(tree, node1, node2.parent);
          } else if(node1.depth > node2.depth) {
            return this.getCommonParentInfo(tree, node1.parent, node2);
          }

          if(node1 === node2) {
            // both nodes are part of the same hierarchy
            return null;
          }

          var parent1 = node1.parent;
          var parent2 = node2.parent;
          if(parent1 === parent2) {
            return {
              parent: parent1,
              child1: node1,
              child2: node2
            };
          }

          return this.getCommonParentInfo(tree, parent1, parent2);
        },

        getDependsOnRelationships: function(nodeTemplate, relationshipTypes) {
          var dependsOnRelationships = toscaService.getRelationships(nodeTemplate, function(relationship) {
            return !toscaService.isHostedOnType(relationship.type, relationshipTypes) && !toscaService.isNetworkType(relationship.type, relationshipTypes);
          });
          return dependsOnRelationships;
        },

        buildTreeGraph: function(tree, nodeMap, relationshipTypes, nodeSize) {
          // Extract our graph with node and links
          var bbox = new UTILS.BoundingBox();
          var graph = {
            nodes : [],
            links : [],
            bbox : bbox
          };
          for (var i = 0; i < tree.children.length; i++) {
            this.addNodeToGraph(graph, tree.children[i], nodeMap, relationshipTypes, nodeSize);
          }
          return graph;
        },

        addNodeToGraph: function(graph, node, nodeMap, relationshipTypes, nodeSize) {
          var i;
          // add the node to the graph
          graph.nodes.push({
            id : node.name,
            coordinate : node.nodeCoordinate,
            bbox: new UTILS.BoundingBox(node.nodeCoordinate.x - nodeSize.halfWidth , node.nodeCoordinate.y - nodeSize.halfHeight , nodeSize.width, nodeSize.height)
          });
          graph.bbox.addRectFromCenter(node.nodeCoordinate.x, node.nodeCoordinate.y, nodeSize.width, nodeSize.height);

          for (i = 0; i < node.attached.length; i++) {
            this.addNodeToGraph(graph, node.attached[i], nodeMap, relationshipTypes, nodeSize);
          }
          for (i = 0; i < node.children.length; i++) {
            this.addNodeToGraph(graph, node.children[i], nodeMap, relationshipTypes, nodeSize);
          }
        },

        treeLayout: function(tree, x, nodeSize, spacing) {
          var position = {x: x, y: 0};
          for (var i = 0; i < tree.children.length; i++) {
            this.nodeLayout(tree.children[i], position, nodeSize, spacing, true);
            position.x += spacing.rootBranch.x + tree.children[i].bbox.width();
            position.y = 0;
          }
        },

        nodeLayout: function(node, position, nodeSize, spacing, initial) {
          node.bbox = new UTILS.BoundingBox(position.x, position.y, nodeSize.width, nodeSize.height);
          node.nodeCoordinate = {x: node.bbox.minX, y: -node.bbox.minY};

          var attachedY = node.bbox.minY - nodeSize.height;
          for(var j = 0; j < node.attached.length; j++) {
            var attached = node.attached[j];
            if(initial) { // append the attached node
              attached.bbox = new UTILS.BoundingBox(node.bbox.minX, attachedY, nodeSize.width, nodeSize.height);
              attached.nodeCoordinate = {x: attached.bbox.minX, y: -attached.bbox.minY};
              attachedY = attached.bbox.minY - nodeSize.height;
              node.bbox.addPoint(attached.bbox.maxX, attached.bbox.maxY);
              node.bbox.addPoint(attached.bbox.minX, attached.bbox.minY);
            } else { // insert the attached node
              node.nodeCoordinate.y = -node.bbox.maxY;
              attached.bbox = new UTILS.BoundingBox(node.bbox.minX, attachedY, nodeSize.width, nodeSize.height);
              attached.nodeCoordinate = {x: attached.bbox.minX, y: -attached.bbox.minY};
              attachedY = attached.bbox.maxY;
              node.bbox.addPoint(attached.bbox.maxX, attached.bbox.maxY);
              node.bbox.addPoint(attached.bbox.minX, attached.bbox.minY);
            }
          }

          var childPosition = {
            x: node.bbox.minX + spacing.branch.x,
            y: node.bbox.maxY + spacing.branch.y
          };

          for(var i = 0; i < node.children.length; i++) {
            var childBbox = this.nodeLayout(node.children[i], childPosition, nodeSize, spacing, false);
            node.bbox.addPoint(childBbox.maxX, childBbox.maxY);
            childPosition.y = childBbox.maxY + spacing.node.y;
          }

          return node.bbox;
        },

        networkLayout: function(tree, graph, nodeSize, spacing) {
          var networkX = graph.bbox.minX - nodeSize.halfWidth - spacing.network;
          var netYSpacing = spacing.network;
          for(var i=0; i<tree.networks.length; i++) {
            var node = tree.networks[i];
            node.nodeCoordinate = {
              x: networkX,
              y: graph.bbox.maxY + nodeSize.halfHeight + netYSpacing
            };
            netYSpacing = 0;
            node.networkId = i;
            graph.nodes.push({
              id : node.name,
              coordinate : node.nodeCoordinate,
              networkId: i,
              bbox: new UTILS.BoundingBox(node.nodeCoordinate.x - nodeSize.halfWidth , node.nodeCoordinate.y - nodeSize.halfHeight , nodeSize.width, nodeSize.height)
            });
            graph.bbox.addRectFromCenter(node.nodeCoordinate.x, node.nodeCoordinate.y, nodeSize.width, nodeSize.height);
          }
        },

        linkLayout: function(graph, nodeTemplates, nodeTypes, relationshipTypes, nodeSize, spacing) {
          var thiss = this;

          nodeTemplates = nodeTemplates || {};
          $.each(nodeTemplates, function(nodeId, node) {
            // For each node map the relationships to the graph.
            thiss.buildGraphRelationships(graph, node, nodeTemplates, relationshipTypes, nodeSize, spacing);
          });
        },

        buildGraphRelationships: function(graph, nodeTemplate, nodeMap, relationshipTypes, nodeSize, spacing) {
          var networkCount = 0;
          var relationships = toscaService.getRelationships(nodeTemplate, function(relationship) {
            return toscaService.isOneOfType(['tosca.relationships.Root'], relationship.type, relationshipTypes);
          });
          // var relationships = getRelationships(node, relationshipTypes);
          if (UTILS.isDefinedAndNotNull(relationships)) {
            var relationshipsLength = relationships.length;
            for (var i = 0; i < relationshipsLength; i++) {
              var relationship = relationships[i];
              var sourceCenter = nodeTemplate.nodeCoordinate;
              var targetCenter = nodeMap[relationship.target].nodeCoordinate;
              var source = {};
              var target = {};
              var isNetwork = false;
              var networkId = null;
              if(toscaService.isAttachedToType(relationship.type, relationshipTypes)) {
                continue;
              }
              if (toscaService.isHostedOnType(relationship.type, relationshipTypes)) {
                source.x = sourceCenter.x - nodeSize.halfWidth - 1;
                source.y = sourceCenter.y + nodeSize.halfHeight - 5;
                target.x = targetCenter.x - nodeSize.halfWidth + 5;
                target.y = targetCenter.y - nodeSize.halfHeight - 1;
                source.direction = routerFactoryService.directions.left;
                target.direction = routerFactoryService.directions.up;
              } else if(toscaService.isNetworkType(relationship.type, relationshipTypes)){
                isNetwork = true;
                networkCount++;
                source.x = nodeTemplate.bbox.minX - nodeSize.halfWidth + spacing.network * networkCount;
                source.y = - nodeTemplate.bbox.minY + nodeSize.halfHeight;
                target.x = source.x;
                target.y = targetCenter.y;
                networkId = nodeMap[relationship.target].networkId;
              } else {
                source.x = sourceCenter.x + nodeSize.halfWidth + 1;
                source.y = sourceCenter.y;
                target.x = targetCenter.x - nodeSize.halfWidth - 1;
                target.y = targetCenter.y;
                source.direction = routerFactoryService.directions.right;
                target.direction = routerFactoryService.directions.left;
              }
              var selected = nodeMap[relationship.target].selected || nodeTemplate.selected;
              graph.links.push({
                id: nodeTemplate.name + '.' + relationship.id,
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
    }]);
