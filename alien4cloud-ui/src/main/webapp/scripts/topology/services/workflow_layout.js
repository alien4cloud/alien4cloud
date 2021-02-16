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
                var edgeData = graph.edge(edge.v, edge.w, edge.name);
                simpleGraph.setEdge(edge.v, edge.w, edgeData, edge.name );
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
              var edgeData = graph.edge(edge.v, edge.w, edge.name);
              var mergeId = graph.node(edge.w).mergeId;
              if(_.undefined(mergeId)) {
                mergeId = edge.w;
              }
              simpleGraph.setEdge(edge.v, mergeId, edgeData, edge.name);
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
            var edge = wrappedGraph.edge(edgeDef.v, edgeDef.w, edgeDef.name);
            if(_.undefined(edge)) {
              var initialEdge = initialGraph.edge(edgeDef.v, edgeDef.w, edgeDef.name);
              initialEdge.source = wrappedGraph.node(edgeDef.v);
              initialEdge.target = wrappedGraph.node(edgeDef.w);
              wrappedGraph.setEdge(edgeDef.v, edgeDef.w, initialEdge, edgeDef.name);
              //unwrappedEdges.push(initialEdge);
              unwrappedEdges.push(edgeDef);
            }
          });

          _.each(unwrappedEdges, function(edgeDef) {
            var outEdges = wrappedGraph.outEdges(edgeDef.v);
            var edge = wrappedGraph.edge(edgeDef.v, edgeDef.w, edgeDef.name);
            var minX = edge.target.x;
            var idx = 0;
            var delta = 0;
            var points;

            _.each(outEdges, function(outEdgeDef) {
              var targetNode = wrappedGraph.node(outEdgeDef.w);
              minX = Math.min(minX, targetNode.x - edge.target.width/2);
            });

            var siblings = wrappedGraph.outEdges(edgeDef.v,edgeDef.w);
            for (; idx < siblings.length ; idx++) {
                                if (siblings[idx].name == edgeDef.name) break;
            }

            if (siblings.length == 2) {
                var sy = edge.source.y - edge.source.height /2 + idx * edge.source.height;
                var ty = edge.target.y - edge.target.height /2 + idx * edge.target.height;

                points = [
                  {x: edge.source.x + edge.source.width/2 - 5, y: sy },
                  {x: minX - 10, y: (sy + ty)/2 + (idx*2 -1) * 10},
                  {x: edge.target.x - edge.target.width/2 + 5, y: ty }
                ];
            } else {

                if (siblings.length > 1) {
                    delta = idx * 20 - siblings.length * 10 + 10;
                }

                points = [
                  {x: edge.source.x + edge.source.width/2, y: edge.source.y + delta / 2},
                  {x: minX - 15, y: edge.source.y + delta / 2},
                  {x: minX - 10, y: (edge.target.y + edge.source.y)/2+delta},
                  {x: minX - 5, y: edge.target.y + delta /2 },
                  {x: edge.target.x - edge.target.width/2, y: edge.target.y + delta / 2}
                ];
            }
            edge.points = points;
          });
        }
      };
    } // function
  ]); // factory
}); // define
