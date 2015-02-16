/* global UTILS */

'use strict';

angular.module('alienUiApp').factory(
    'topologyLayoutService', ['toscaService',
    function(toscaService) {
      /**
       * Step 1 - Build the tree structure for our topology based on hosted on
       * relationship.
       */
      var buildHostedOnTree = function(nodeMap, relationshipTypes) {
        return buildTree(nodeMap, relationshipTypes, getHostedOnRelationship);
      };

      var getHostedOnRelationship = function(node, relationshipTypes) {
        var hostedOnRelationship = searchRelationship(node, function(relationship) {
          return toscaService.isHostedOnType(relationship.type, relationshipTypes) || toscaService.isNetworkType(relationship.type, relationshipTypes);
        });
        return hostedOnRelationship;
      };

      var getDependsOnRelationship = function(node, relationshipTypes) {
        var dependsOnRelationship = searchRelationship(node, function(relationship) {
          return !toscaService.isHostedOnType(relationship.type, relationshipTypes) && !toscaService.isNetworkType(relationship.type, relationshipTypes);
        });
        return dependsOnRelationship;
      };

      var buildTree = function(nodeMap, relationshipTypes, getRelationship) {
        var rootNode = {
          'children' : []
        };
        if (!nodeMap) {
          return rootNode;
        }
        for ( var nodeName in nodeMap) {
          if (nodeMap.hasOwnProperty(nodeName)) {
            var currentNode = nodeMap[nodeName];
            currentNode.name = nodeName;
            var relationships = getRelationship(currentNode, relationshipTypes);
            if (relationships.length > 0) {
              // Only take into account first element of the relationships found
              // It's enough to build the tree
              var parent = nodeMap[relationships[0].target];
              if (parent.hasOwnProperty('children')) {
                parent.children.push(currentNode);
                currentNode.parent = parent;
              } else {
                parent.children = [currentNode];
                currentNode.parent = parent;
              }
            } else {
              rootNode.children.push(currentNode);
              currentNode.parent = rootNode;
            }
          }
        }
        return rootNode;
      };

      /**
       * Step 2 - Calculate the depth for each node of our nodeMap Use DFS to
       * calculate node depth
       */
      var calculateNodeDepth = function(rootNode) {
        var visitedNodes = {};
        doCalculateNodeDepth(rootNode, visitedNodes, -1);
      };

      var doCalculateNodeDepth = function(node, visitedNodes, parentDepth) {
        if (node.name in visitedNodes) {
          // Do not visit an already visited node to prevent cyclic dependencies
          return;
        }
        visitedNodes[node.name] = node;
        node.nodeDepth = parentDepth + 1;
        if (UTILS.isDefinedAndNotNull(node.children)) {
          for (var i = 0; i < node.children.length; i++) {
            doCalculateNodeDepth(node.children[i], visitedNodes, node.nodeDepth);
          }
        }
      };

      /**
       * Step 3 - Calculate the horizontal weight for each node of our topology.
       * More a node has higher horizontal weight, more it will be on the right
       * hand side of the graph
       */
      var calculateHorizontalWeight = function(rootNode, nodeMap, relationshipTypes, externalLinkWeight) {
        var branches = [];
        if (!UTILS.isDefinedAndNotNull(rootNode.children)) {
          return;
        }
        var rootNodeChildrenLength = rootNode.children.length;
        var i, currentBranch, nodeName;
        for (i = 0; i < rootNodeChildrenLength; i++) {
          rootNode.children[i].branchHorizontalWeight = -Number.MAX_VALUE;
          branches.push(flattenTreeStructure(rootNode.children[i]));
        }
        var branchesLength = branches.length;
        // Initialize left weight and right weight for each node
        for (i = 0; i < branchesLength; i++) {
          currentBranch = branches[i];
          for (nodeName in currentBranch.nodes) {
            if (currentBranch.nodes.hasOwnProperty(nodeName)) {
              currentBranch.nodes[nodeName].nodeHorizontalWeight = -Number.MAX_VALUE;
            }
          }
        }

        // Calculate horizontal weight
        for (i = 0; i < branchesLength; i++) {
          currentBranch = branches[i];
          for (nodeName in currentBranch.nodes) {
            if (currentBranch.nodes.hasOwnProperty(nodeName)) {
              computeNodeHorizontalWeight(nodeMap, relationshipTypes, externalLinkWeight, i, branches, currentBranch, nodeName);
            }
          }
        }
      };

      var computeNodeHorizontalWeight = function(nodeMap, relationshipTypes, externalLinkWeight, i, branches, currentBranch, nodeName) {
        var dependsOnRelationships = getDependsOnRelationship(currentBranch.nodes[nodeName], relationshipTypes);
        var dependsOnRelationshipsLength = dependsOnRelationships.length;
        if (dependsOnRelationshipsLength > 0) {
          // I'm the source of some depends on relationship
          var source = currentBranch.nodes[nodeName];
          for (var j = 0; j < dependsOnRelationshipsLength; j++) {
            var targetName = dependsOnRelationships[j].target;
            var target = nodeMap[targetName];
            var weightToAdd;
            if (targetName in currentBranch.nodes) {
              weightToAdd = 1;
            } else {
              var targetBranch = findBranchContainingNode(branches, targetName, i);
              if (targetBranch.root.branchHorizontalWeight === -Number.MAX_VALUE) {
                targetBranch.root.branchHorizontalWeight = 0;
              }
              if (currentBranch.root.branchHorizontalWeight === -Number.MAX_VALUE) {
                currentBranch.root.branchHorizontalWeight = 0;
              }
              targetBranch.root.branchHorizontalWeight += 1;
              currentBranch.root.branchHorizontalWeight -= 1;
              // Add more weight if it's external link
              weightToAdd = externalLinkWeight;
            }
            if (source.nodeHorizontalWeight === -Number.MAX_VALUE) {
              source.nodeHorizontalWeight = 0;
            }
            if (target.nodeHorizontalWeight === -Number.MAX_VALUE) {
              target.nodeHorizontalWeight = 0;
            }
            source.nodeHorizontalWeight += weightToAdd;
            target.nodeHorizontalWeight -= weightToAdd;
          }
        }
      };

      /**
       * Step 4 - Calculate the layout based on vertical and horizontal weight
       * that we already calculated
       */
      var calculateLayout = function(rootNode, nodeMap, relationshipTypes, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
          distanceBetweenNodeVertical) {
        var nodeName, node;
        // Sort the branches
        rootNode.children.sort(compareBranchHorizontalWeight);
        // Sort the tree based on the horizontal weight
        for (nodeName in nodeMap) {
          node = nodeMap[nodeName];
          if (node !== rootNode) {
            // Sort only nodes which are not root of branches
            // Branches are already sorted above
            var children = node.children;
            if (UTILS.isDefinedAndNotNull(children)) {
              children.sort(compareNodeHorizontalWeight);
            }
          }
        }
        // After this call we have the tree with every element filled with
        // coordinate. We start from nodeWidth / 2 to have the rectangle inside
        // the viewbox
        doCalculateLayout(rootNode, rootNode, nodeWidth / 2, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
            distanceBetweenNodeVertical);
        // Extract our graph with node and links
        var bbox = new UTILS.BoundingBox();
        var graph = {
          nodes : [],
          links : [],
          bbox : bbox
        };
        // We might save the graph (coordinates) on server side to
        // save the state of a topology graph
        for (nodeName in nodeMap) {
          node = nodeMap[nodeName];
          graph.nodes.push({
            id : node.name,
            coordinate : node.nodeCoordinate,
            bbox: new UTILS.BoundingBox(node.nodeCoordinate.x - nodeWidth/2 , node.nodeCoordinate.y - nodeHeight/2 , nodeWidth, nodeHeight)
          });

          graph.bbox.addRectFromCenter(node.nodeCoordinate.x, node.nodeCoordinate.y, nodeWidth, nodeHeight);

          buildGraphRelationships(graph, node, nodeMap, relationshipTypes, getHostedOnRelationship, nodeWidth, nodeHeight);
          buildGraphRelationships(graph, node, nodeMap, relationshipTypes, getDependsOnRelationship, nodeWidth, nodeHeight);
        }

        return graph;
      };

      var buildGraphRelationships = function(graph, node, nodeMap, relationshipTypes, getRelationships, nodeWidth, nodeHeight) {
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
              // if (sourceCenter.x > targetCenter.x) {
              //   source.x = sourceCenter.x - nodeWidth / 2 - 1;
              //   target.x = targetCenter.x + nodeWidth / 2 + 1;
              // } else {
              source.x = sourceCenter.x + nodeWidth / 2;
              target.x = targetCenter.x - nodeWidth / 2;
              // }
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
      };

      var doCalculateLayout = function(node, rootNode, currentX, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
          distanceBetweenNodeVertical) {
        node.nodeCoordinate = {};
        node.nodeCoordinate.y = node.nodeDepth * (nodeHeight + distanceBetweenNodeVertical) + nodeHeight / 2;
        var children = node.children;
        var newX = currentX;
        if (UTILS.isDefinedAndNotNull(children)) {
          for (var i = 0; i < children.length; i++) {
            newX = doCalculateLayout(children[i], rootNode, newX, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
                distanceBetweenNodeVertical);
            if (i !== children.length - 1) {
              if (node === rootNode) {
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
      };

      var compareNodeHorizontalWeight = function(left, right) {
        if (left.nodeHorizontalWeight < right.nodeHorizontalWeight) {
          return -1;
        }
        if (left.nodeHorizontalWeight > right.nodeHorizontalWeight) {
          return 1;
        }
        return 0;
      };

      var compareBranchHorizontalWeight = function(left, right) {
        if (left.branchHorizontalWeight < right.branchHorizontalWeight) {
          return -1;
        }
        if (left.branchHorizontalWeight > right.branchHorizontalWeight) {
          return 1;
        }
        return 0;
      };

      var findBranchContainingNode = function(branches, nodeName, excludeIndex) {
        var branchLenght = branches.length;
        for (var i = 0; i < branchLenght; i++) {
          if (i === excludeIndex) {
            continue;
          }
          if (nodeName in branches[i].nodes) {
            return branches[i];
          }
        }
      };

      /**
       * Use BFS to flatten tree structure
       */
      var flattenTreeStructure = function(rootNode) {
        var branchNodes = {};
        var queue = [rootNode];
        var node;
        while (UTILS.isDefinedAndNotNull(node = queue.shift())) {
          // Only consider a node if it's not yet processed to prevent cyclic
          // dependencies
          if (!(node.name in branchNodes)) {
            branchNodes[node.name] = node;
            if (UTILS.isDefinedAndNotNull(node.children)) {
              queue = queue.concat(node.children);
            }
          }
        }
        return {
          root : rootNode,
          nodes : branchNodes
        };
      };

      /**
       * Search and return the node's relationship with defined criteria. Do not
       * consider multiple relationship of the same type for the moment.
       */
      var searchRelationship = function(node, criteria) {
        var founds = [];
        if (UTILS.isDefinedAndNotNull(node.relationships)) {
          var relationships = node.relationships;
          for(var i=0;i<node.relationships.length;i++) {
            var relationship = node.relationships[i].value;
            relationship.id = node.relationships[i].key;
            if (criteria(relationship)) {
              founds.push(relationship);
            }
          }
        }
        return founds;
      };

      var layout = function(nodeMap, relationshipTypes, externalLinkWeight, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
          distanceBetweenNodeVertical) {
        var copy = UTILS.deepCopy(nodeMap);
        var tree = this.buildHostedOnTree(copy, relationshipTypes);
        this.calculateNodeDepth(tree);
        this.calculateHorizontalWeight(tree, copy, relationshipTypes, externalLinkWeight);
        return this.calculateLayout(tree, copy, relationshipTypes, nodeWidth, nodeHeight, distanceBetweenBranchHorizontal, distanceBetweenNodeHorizontal,
            distanceBetweenNodeVertical);
      };

      return {
        'buildHostedOnTree' : buildHostedOnTree,
        'calculateNodeDepth' : calculateNodeDepth,
        'calculateHorizontalWeight' : calculateHorizontalWeight,
        'calculateLayout' : calculateLayout,
        'layout' : layout
      };
    }]);
