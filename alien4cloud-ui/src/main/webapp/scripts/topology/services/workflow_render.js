define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var dagre = require('dagre');

  require('scripts/topology/services/workflow_layout');

  modules.get('a4c-topology-editor').factory('planRender', ['d3Service', 'workflowShapes', 'planLayout', 'bboxFactory',
    function(d3Service, workflowShapes, planLayout, bboxFactory) {
      // This service render a graph using d3 js.
      return {
        // Create the renderer
        createGraph: function() {
          var graph = new dagre.graphlib.Graph({
            compound : true
          }).setGraph({}).setDefaultEdgeLabel(function() {
            return {};
          });
          // Configure horizontal layout
          graph.graph().rankdir = 'LR';
          graph.graph().ranksep = 20;
          return graph;
        },

        /**
        * Performs layout on the input graph.
        *
        * This rendering is inspired from dagre-d3 but simplifies the global algorithm for our use-case.
        * It also provide some graph simplification to improve layout calculations.
        */
        render: function(svg, graph, layout) {
          var renderedGraph = graph;
          if(layout) { // Performs layout and draw the graph
            // we simplifies the graph so dagre process the layout faster
            var simpleGraph = this.createGraph();
            planLayout.simplify(graph, simpleGraph, 'end');
            // add hosts back
            //var clusters = graph.nodes().filter(function(v) { return !!graph.children(v).length; });
            //_.each(clusters, function(clusterKey) {
            //  simpleGraph.setNode(clusterKey, graph.node(clusterKey));
            //});
            // dagre layout
            dagre.layout(simpleGraph, {debugTiming: false});
            // unwrap simplified nodes
            planLayout.unwrap(simpleGraph, 'end');
            planLayout.unwrapEdges(simpleGraph, graph);
            renderedGraph = simpleGraph;
            var bbox = bboxFactory.create();
            // cleanup layout data from graph
            _.each(graph.nodes(), function(nodeKey) {
              var node = graph.node(nodeKey);
              node.id = nodeKey;
              delete node.simplified;
              delete node.merged;
              delete node.unwraped;
              delete node.mergeId;
              bbox.addPoint(node.x, node.y);
              bbox.addPoint(node.x + node.width, node.y + node.height);
            });
            this.bbox = bbox;
          }

          // prepare groups to draw clusters, edges and nodes.
//          var clustersGroup = this.createOrSelectGroup(svg, 'clusters'),
          var  edgesGroup = this.createOrSelectGroup(svg, 'edges'),
            nodesGroup = this.createOrSelectGroup(svg, 'nodes');

          // draw nodes
          this.createNodes(nodesGroup, renderedGraph);
//          this.createClusters(clustersGroup, renderedGraph);
          this.createEdges(edgesGroup, renderedGraph);
        },

        createOrSelectGroup: function(root, name) {
          var selection = root.select('g.' + name);
          if (selection.empty()) {
            selection = root.append('g').attr('class', name);
          }
          return selection;
        },

        createNodes: function(selection, graph) {
          // don't process clusters
          var simpleNodeKeys = graph.nodes().filter(function(v) { return !graph.children(v).length; });
          // let's just create an array of nodes
          var simpleNodes = [];
          _.each(simpleNodeKeys, function(nodeKey) {
            simpleNodes.push(graph.node(nodeKey));
          });
          // create and update nodes - selection is based on id field by default.
          d3Service.select(selection, simpleNodes, 'g.node', {
            enter: function(enterSelection) {
              return enterSelection.append('g').attr('class', 'node');
            },
            create: function(group, node) {
              if (node.id) {
                group.attr('id', node.id);
              }
              if(_.defined(workflowShapes[node.shape])) {
                workflowShapes[node.shape](group, node);
              } else {
                console.error('unknown node', node.shape);
              }
            },
            update: function(group, node) {
              // update position of the node
              group.attr('transform', 'translate('+node.x+','+node.y+')');
              // update the shape
              var shapeUpdate = node.shape+'Update';
              if(_.defined(workflowShapes[shapeUpdate])) {
                workflowShapes[shapeUpdate](group, node);
              }
            }
          });
        },

        createClusters: function(selection, graph) {
          var clusters = graph.nodes().filter(function(v) { return !!graph.children(v).length; });
          var clusterNodes = [];
          _.each(clusters, function(nodeKey) {
            var cluster = graph.node(nodeKey);
            if(_.defined(cluster)) {
              clusterNodes.push();
            }
          });
          d3Service.select(selection, clusterNodes, 'g.cluster', {
            enter: function(enterSelection) {
              return enterSelection.append('g').attr('class', 'cluster');
            },
            create: function(group, node) {
              if (node.id) {
                group.attr('id', node.label);
              }
              d3Service.rect(group, - node.width/2, - node.height/2, node.width, node.height, 0, 0);
            },
            update: function(group, node) {
              // update cluster position
              group.attr('transform', 'translate('+node.x+','+node.y+')');
              // update the cluster size
              var rect = group.select('rect');
              rect.attr('x', - node.width/2)
                .attr('y', - node.height/2)
                .attr('width', node.width)
                .attr('height', node.height);
            }
          }, function(node){ return node.label; }); // label is the id of cluster nodes
        },

        createEdges: function(selection, graph) {
          var instance = this;
          var edges = [];
          _.each(graph.edges(), function(edgeDef) {
            var edge = graph.edge(edgeDef.v, edgeDef.w);
            edge.id = instance.edgeToId(edgeDef);
            edge.source = graph.node(edgeDef.v);
            edge.target = graph.node(edgeDef.w);
            edges.push(edge);
          });

          d3Service.select(selection, edges, 'g.edgePath', {
            enter: function(enterSelection) {
              return enterSelection.append('g').attr('class', 'edgePath');
            },
            create: function(group, edge) {
              if (edge.id) {
                group.attr('id', edge.id);
              }
              workflowShapes.edge(group);
            },
            update: function(group) {
              workflowShapes.edgeUpdate(group);
            }
          });
        },

        edgeToId: function(e) {
          return this.escapeId(e.v) + ':' + this.escapeId(e.w) + ':' + this.escapeId(e.name);
        },

        ID_DELIM: /:/g,
        escapeId: function(str) {
          return str ? String(str).replace(this.ID_DELIM, '\\:') : '';
        }
      };
    } // function
  ]); // factory
}); // define
