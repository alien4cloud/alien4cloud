/* global UTILS, $ */

'use strict';

angular.module('alienUiApp').factory(
    'topologyLayoutService', ['toscaService',
    function(toscaService) {
      return {
        layout: function(nodeTemplates, relationshipTypes, externalLinkWeight, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
          distanceBetweenNodeVertical) {
          var nodeTemplatesCopy = UTILS.deepCopy(nodeTemplates);
          var tree = this.buildTree(nodeTemplatesCopy, relationshipTypes);
          this.sortTree(tree, relationshipTypes, nodeTemplatesCopy);
          return this.calculateLayout(tree, nodeTemplatesCopy, relationshipTypes, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
              distanceBetweenNodeVertical);
        },

        /**
        * Build a tree of nodes based on tosca relationships.
        *
        * @param nodeTemplates The map of node templates in the topology.
        * @param relationshipTypes Map of relationship types used in the topology.
        */
        buildTree: function(nodeTemplates, relationshipTypes) {
          var thiss = this;
          // define the root of the tree that will be returned
          var tree = {
            'children' : []
          };
          if(!nodeTemplates) {
            return tree;
          }
          $.each(nodeTemplates, function(nodeId, nodeTemplate) {
            nodeTemplate.name = nodeId;
            if(UTILS.isUndefinedOrNull(nodeTemplate.children)) {
              nodeTemplate.children = [];
            }
            nodeTemplate.weight = 0;
            // get relationships that we want to display as hosted on.
            var relationships = thiss.getHostedOnRelationships(nodeTemplate, relationshipTypes);
            if (relationships.length > 0) {
              // TODO we should not have more than a single hosted on actually. Manage if not.
              var parent = nodeTemplates[relationships[0].target];
              UTILS.safePush(parent, 'children', nodeTemplate);
              nodeTemplate.parent = parent;
            } else {
              tree.children.push(nodeTemplate);
              nodeTemplate.parent = tree;
            }
          });

          var visitedNodes = {};
          this.computeNodeDepth(tree, visitedNodes, -1);

          return tree;
        },

        /**
        * Quickly go over the tree to add a depth field to every node (this will be used for sorting later on).
        */
        computeNodeDepth: function(node, visitedNodes, parentDepth) {
          if (node.name in visitedNodes) {
            // Do not visit an already visited node to prevent cyclic dependencies
            console.log('Cyclic dependency detected, topology may not be valid.');
            return;
          }
          visitedNodes[node.name] = node;
          node.depth = parentDepth + 1;
          for (var i = 0; i < node.children.length; i++) {
            this.computeNodeDepth(node.children[i], visitedNodes, node.depth);
          }
        },

        /**
        * Get all hosted on or network relationships on a given node template.
        */
        getHostedOnRelationships: function(nodeTemplate, relationshipTypes) {
          var hostedOnRelationships = toscaService.getRelationships(nodeTemplate, function(relationship) {
            return toscaService.isHostedOnType(relationship.type, relationshipTypes) || toscaService.isNetworkType(relationship.type, relationshipTypes);
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

        calculateLayout: function(tree, nodeMap, relationshipTypes, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
          distanceBetweenNodeVertical) {
          var nodeName, node;
          // After this call we have the tree with every element filled with coordinate. We start from nodeWidth / 2 to have the rectangle inside
          // the viewbox
          this.doCalculateLayout(tree, tree, nodeWidth / 2, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
              distanceBetweenNodeVertical);
          // Extract our graph with node and links
          var bbox = new UTILS.BoundingBox();
          var graph = {
            nodes : [],
            links : [],
            bbox : bbox
          };
          for (nodeName in nodeMap) {
            node = nodeMap[nodeName];
            graph.nodes.push({
              id : node.name,
              coordinate : node.nodeCoordinate,
              bbox: new UTILS.BoundingBox(node.nodeCoordinate.x - nodeWidth/2 , node.nodeCoordinate.y - nodeHeight/2 , nodeWidth, nodeHeight)
            });

            graph.bbox.addRectFromCenter(node.nodeCoordinate.x, node.nodeCoordinate.y, nodeWidth, nodeHeight);

            this.buildGraphRelationships(graph, node, nodeMap, relationshipTypes, this.getHostedOnRelationships, nodeWidth, nodeHeight);
            this.buildGraphRelationships(graph, node, nodeMap, relationshipTypes, this.getDependsOnRelationships, nodeWidth, nodeHeight);
          }

          return graph;
        },

        doCalculateLayout: function(node, tree, currentX, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
          distanceBetweenNodeVertical) {
          node.nodeCoordinate = {};
          node.nodeCoordinate.y = node.depth * (nodeHeight + distanceBetweenNodeVertical) + nodeHeight / 2;
          var children = node.children;
          var newX = currentX;
          if (children.length > 0) {
            for (var i = 0; i < children.length; i++) {
              newX = this.doCalculateLayout(children[i], tree, newX, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
                  distanceBetweenNodeVertical);
              if (i !== children.length - 1) {
                if (node === tree) {
                  // I'm a branch root so must apply the
                  // distanceBetweenBranchHorizontal
                  newX += distanceBetweenBranchHorizontal;
                } else {
                  newX += distanceBetweenNodeHorizontal;
                }
              }
            }
            node.nodeCoordinate.x = (newX + currentX - nodeWidth) / 2;
          } else {
            node.nodeCoordinate.x = currentX;
            newX += nodeWidth;
          }
          return newX;
        },

        buildGraphRelationships: function(graph, node, nodeMap, relationshipTypes, getRelationships, nodeWidth, nodeHeight) {
          var relationships = getRelationships(node, relationshipTypes);
          if (UTILS.isDefinedAndNotNull(relationships)) {
            var relationshipsLength = relationships.length;
            for (var i = 0; i < relationshipsLength; i++) {
              var relationship = relationships[i];
              var sourceCenter = node.nodeCoordinate;
              var targetCenter = nodeMap[relationship.target].nodeCoordinate;
              var source = {};
              var target = {};
              if (toscaService.isHostedOnType(relationship.type, relationshipTypes) || toscaService.isNetworkType(relationship.type, relationshipTypes)) {
                if (sourceCenter.y > targetCenter.y) {
                  source.y = sourceCenter.y - nodeHeight / 2;
                  target.y = targetCenter.y + nodeHeight / 2;
                } else {
                  source.y = sourceCenter.y + nodeHeight / 2;
                  target.y = targetCenter.y - nodeHeight / 2;
                }
                source.x = sourceCenter.x;
                target.x = targetCenter.x;
              } else {
                source.x = sourceCenter.x + nodeWidth / 2;
                target.x = targetCenter.x - nodeWidth / 2;
                source.y = sourceCenter.y;
                target.y = targetCenter.y;
              }
              var selected = nodeMap[relationship.target].selected || node.selected;
              graph.links.push({
                id: node.name + '.' + relationship.id,
                type: relationship.type,
                source: source,
                target: target,
                selected: selected
              });
            }
          }
        }
      };
    }]);
