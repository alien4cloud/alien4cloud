define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('planLayout', [ function() {
      // This service performs layout on a simple directed graph.
      return {
        spacing: 20,

        /**
        * Replace a set of nodes that are linked only together (single successor) by a single node
        */
        simplify: function(graph, simpleGraph, nodeKey) {
          this.doSimplify(graph, simpleGraph, nodeKey);
          this.addUnprocessed(graph, simpleGraph);
        },

        addUnprocessed: function(graph, simpleGraph) {
          _.each(graph.nodes(), function(nodeKey) {
            var node = graph.node(nodeKey);
            if(_.undefined(node.simplified) || !node.simplified) {
              simpleGraph.setNode(nodeKey, node);
              // add all edges from the unprocessed node (not connected to end).
              var edges = graph.outEdges(nodeKey);
              _.each(edges, function(edge) {
                var edgeData = graph.edge(edge.v, edge.w);
                simpleGraph.setEdge(edge.v, edge.w, edgeData);
              });
            }
          });
        },

        /**
        * Replace a set of nodes that are linked only together (single successor) by a single node
        */
        doSimplify: function(graph, simpleGraph, nodeKey) {
          // merge nodes that can be merged all together
          var instance = this;
          // if I have a single successor and the successor is in the same parent group
          var successors = graph.successors(nodeKey);
          // do not process node as long as all successors have not been processed
          var successorsProcessed = true;
          if(_.defined(successors)) {
            _.each(successors, function(successorKey) {
              if(!graph.node(successorKey).simplified) {
                successorsProcessed = false;
              }
            });
          }
          if(!successorsProcessed) {
            return;
          }
          var node = graph.node(nodeKey);
          node.id = nodeKey;
          node.simplified = true;
          if(_.defined(successors) && successors.length === 1 && graph.predecessors(successors[0]).length === 1 &&
            graph.parent(nodeKey) === graph.parent(successors[0])) {
            // nodes can be merged
            var successor = graph.node(successors[0]);
            var simpleNodeId = successor.mergeId;
            var simpleNode = simpleGraph.node(simpleNodeId);
            if(_.defined(simpleNode) &&_.defined(simpleNode.merged)) {
              // the successor node is already a merge node
              node.mergeId = simpleNodeId;
              simpleNode.merged.push(node);
              simpleNode.width += this.spacing + node.width;
              simpleNode.height = Math.max(simpleNode.height, node.height);
            } else {
              // we have to update the successor with the merge node
              var width = successor.width + this.spacing + node.width;
              var height = Math.max(successor.height, node.height);
              node.mergeId = successors[0];
              simpleGraph.setNode(successors[0], {
                label : '',
                width: width,
                height: height,
                shape: 'test',
                parent: successor.parent,
                merged: [successor, node]
              });
//              if(_.defined(successor.parent)) {
//                simpleGraph.setParent(successors[0], successor.parent);
//              }
            }
          } else {
            // add non-merged node
            simpleGraph.setNode(nodeKey, node);
//            if(_.defined(node.parent)) {
//              simpleGraph.setParent(nodeKey, node.parent);
//            }
            // add edges to graph
            var edges = graph.outEdges(nodeKey);
            _.each(edges, function(edge) {
              var edgeData = graph.edge(edge.v, edge.w);
              var mergeId = graph.node(edge.w).mergeId;
              if(_.undefined(mergeId)) {
                mergeId = edge.w;
              }
              simpleGraph.setEdge(edge.v, mergeId, edgeData);
            });
          }
          var predecessors = graph.predecessors(nodeKey);
          _.each(predecessors, function(predecessorKey) {
            instance.doSimplify(graph, simpleGraph, predecessorKey);
          });
        },


        unwrap: function(graph, nodeKey) {
          // merge nodes that can be merged all together
          var instance = this;
          // if I have a single successor and the successor is in the same parent group
          var successors = graph.successors(nodeKey);
          // do not process node as long as all successors have not been processed
          var successorsProcessed = true;
          if(_.defined(successors)) {
            _.each(successors, function(successorKey) {
              if(!graph.node(successorKey).unwraped) {
                successorsProcessed = false;
              }
            });
          }
          if(!successorsProcessed) {
            return;
          }
          var node = graph.node(nodeKey);
          node.id = nodeKey;
          node.unwraped = true;

          var predecessors = graph.predecessors(nodeKey);
          if(_.defined(node.merged)) { // unwrap the node
            // remove the node from the graph
            graph.removeNode(nodeKey);
            // put back every single node
            var x = node.x + node.width/2;
            // var successor = null;
            _.each(node.merged, function(mergedNode) {
              // update coordinates of mergedNode
              mergedNode.x = x - mergedNode.width/2;
              x = x - mergedNode.width - instance.spacing;
              mergedNode.y = node.y;
              graph.setNode(mergedNode.id, mergedNode);
//              graph.setParent(mergedNode.id, mergedNode.parent);
            });
          }

          _.each(predecessors, function(predecessorKey) {
            instance.unwrap(graph, predecessorKey);
          });
        },

        unwrapEdges: function(wrappedGraph, initialGraph) {
          var unwrappedEdges = [];
          _.each(initialGraph.edges(), function(edgeDef) {
            var edge = wrappedGraph.edge(edgeDef.v, edgeDef.w);
            if(_.undefined(edge)) {
              var initialEdge = initialGraph.edge(edgeDef.v, edgeDef.w);
              initialEdge.source = wrappedGraph.node(edgeDef.v);
              initialEdge.target = wrappedGraph.node(edgeDef.w);
              wrappedGraph.setEdge(edgeDef.v, edgeDef.w, initialEdge);
              unwrappedEdges.push(initialEdge);
            }
          });
          _.each(unwrappedEdges, function(edge) {
            // compute inflexion point by getting the minX of the source successors
            var outEdges = wrappedGraph.outEdges(edge.source.id);
            var minX = edge.target.x;
            _.each(outEdges, function(outEdgeDef) {
              var targetNode = wrappedGraph.node(outEdgeDef.w);
              minX = Math.min(minX, targetNode.x - edge.target.width/2);
            });

            var points = [
              {x: edge.source.x + edge.source.width/2, y: edge.source.y},
              {x: minX - 15, y: edge.source.y},
              {x: minX - 5, y: edge.target.y},
              {x: edge.target.x - edge.target.width/2, y: edge.target.y}
            ];
            edge.points = points;
          });
        }
      };
    } // function
  ]); // factory
}); // define
