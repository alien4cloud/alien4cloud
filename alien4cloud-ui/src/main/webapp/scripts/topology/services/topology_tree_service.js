/*
This service is used to create a sorted tree of nodes based on relationships.
*/
define(function (require) {
  'use strict';

  var modules = require('modules');
  var $ = require('jquery');
  var _ = require('lodash');

  require('scripts/tosca/services/tosca_service');
  require('scripts/tosca/services/tosca_relationships_service');

  require('scripts/common-graph/services/router_factory_service');
  require('scripts/common-graph/services/bbox_factory_service');

  modules.get('a4c-topology-editor', ['a4c-tosca']).factory('topologyTreeService', ['toscaService', 'toscaRelationshipsService',
    function(toscaService, toscaRelationshipsService) {
      return {
        /**
        * Create an enriched node that we will use to render the topology.
        */
        prepareNode: function(nodeTemplate, topology) {
          var nodeType = topology.nodeTypes[nodeTemplate.type];
          var node = {
            template: nodeTemplate,
            type: nodeType,
            capabilities: [],
            capabilitiesMap: {},
            requirements: [],
            requirementsMap: {}
          };
          var capabilityTypes = topology.capabilityTypes;
          this.processConnector(node, nodeType.capabilities, node.capabilities, node.capabilitiesMap, capabilityTypes, topology.relationshipTypes);
          this.processConnector(node, nodeType.requirements, node.requirements, node.requirementsMap, capabilityTypes, topology.relationshipTypes);
          return node;
        },

        processConnector: function(node, connectors, array, map, capabilityTypes, relationshipTypes) {
          _.each(connectors, function(connector) {
            if(!toscaService.isOneOfType(['tosca.capabilities.Container', 'tosca.capabilities.Attachment', 'tosca.capabilities.Scalable'], connector.type, capabilityTypes) && !(_.defined(connector.relationshipType) && toscaService.isHostedOnType(connector.relationshipType, relationshipTypes))) {
              connector = {
                id: connector.id,
                template: connector,
                type: capabilityTypes[connector.type],
                node: node
              };
              array.push(connector);
              map[connector.id] = connector;
            }
          });
        },

        /**
        * Build a tree of nodes based on tosca relationships.
        *
        * @param nodeTemplates The map of node templates in the topology.
        * @param relationshipTypes Map of relationship types used in the topology.
        */
        buildTree: function(nodeTemplates, topology) {
          var self = this;
          // define the root of the tree that will be returned
          var tree = {
            children : [],
            networks: [],
            nodeMap: {}
          };
          if(!nodeTemplates) {
            return tree;
          }
          _.each(nodeTemplates, function(nodeTemplate, nodeId) {
            var node = self.prepareNode(nodeTemplate, topology);
            node.id = nodeId;
            tree.nodeMap[nodeId] = node;
          });
          _.each(tree.nodeMap, function(node) {
            // network are not managed like other nodes
            if(toscaService.isOneOfType(['tosca.nodes.Network'], node.template.type, topology.nodeTypes)) {
              tree.networks.push(node);
            } else {
              self.addNodeTemplateToTree(tree, node, tree.nodeMap, topology.relationshipTypes);
            }
          });

          var visitedNodes = {};
          for(var i=0;i<tree.children.length;i++) {
            this.computeNodeDepth(tree.children[i], visitedNodes, 0);
          }
          return tree;
        },

        addNodeTemplateToTree: function(tree, node, nodes, relationshipTypes) {
          if(_.undefined(node.children)) {
            node.children = [];
          }
          if(_.undefined(node.attached)) {
            node.attached = [];
          }
          node.weight = 0;
          // get relationships that we want to display as hosted on.
          var relationships = toscaRelationshipsService.getHostedOnRelationships(node.template, relationshipTypes);
          if (relationships.length > 0) {
            // TODO we should not have more than a single hosted on actually. Manage if not.
            var parent = nodes[relationships[0].target];
            _.safePush(parent, 'children', node);
            node.parent = parent;
          } else {
            // Manage the attach relationship in a specific way in order to display the storage close to the compute.
            relationships = toscaRelationshipsService.getAttachedToRelationships(node.template, relationshipTypes);
            if (relationships.length > 0) {
              var target = nodes[relationships[0].target];
              _.safePush(target, 'attached', node);
              node.parent = tree;
              node.isAttached = true;
            } else {
              // if the node is not hosted on another node just add it to the root.
              tree.children.push(node);
              node.parent = tree;
            }
          }
        },

        /**
        * Quickly go over the tree to add a depth field to every node (this will be used for sorting later on).
        */
        computeNodeDepth: function(node, visitedNodes, parentDepth) {
          if (node.id in visitedNodes) {
            // Do not visit an already visited node to prevent cyclic dependencies
            console.error('Hosted on cyclic dependency detected, topology may not be valid.');
            return;
          }
          visitedNodes[node.id] = node;
          node.depth = parentDepth + 1;
          $.each(node.attached, function(index, value){
            value.depth = node.depth;
          });
          for (var i = 0; i < node.children.length; i++) {
            this.computeNodeDepth(node.children[i], visitedNodes, node.depth);
          }
        },

        /**
        * Sort the tree based on depends on relationships that connects nodes to each others.
        *
        * Each group from the root is displayed horizontally while children are displayed vertically.
        */
        sortTree: function(tree, topology) {
          var self = this;
          // compute nodes weight and attractions.
          _.each(tree.children, function(node) {
            self.nodeWeight(tree, topology, node);
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
            if(_.defined(child.before)) { // ensure the node is added before.
              for(var j=0; j<children.length && candidatePosition === -1; j++) {
                if(child.before.indexOf(children[j]) >= 0) {
                  candidatePosition = j;
                }
              }
            }
            if(_.defined(child.after)) { // ensure the node is added after.
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
        nodeWeight: function (tree, topology, node) {
          var self = this;
          // process child nodes
          _.each(node.children, function(node){
            self.nodeWeight(tree, topology, node);
          });
          // process current node
          var relationships = toscaRelationshipsService.getDependsOnRelationships(node.template, topology.relationshipTypes);
          _.each(relationships, function(relationship){
            var commonParentInfo = self.getCommonParentInfo(tree, node, tree.nodeMap[relationship.target]);
            if(commonParentInfo !== null) {
              commonParentInfo.child2.weight += 10000;
              _.safePush(commonParentInfo.child2, 'after', commonParentInfo.child1);
              _.safePush(commonParentInfo.child1, 'before', commonParentInfo.child2);
            } else {
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
        }
      };
    }
  ]); // factory
});// define
