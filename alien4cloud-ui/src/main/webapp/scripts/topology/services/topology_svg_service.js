define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var d3Tip = require('d3-tip');
  var d3 = require('d3');

  require('scripts/common-graph/services/svg_service');
  require('scripts/common-graph/services/d3_service');
  require('scripts/common-graph/services/router_factory_service');

  require('scripts/tosca/services/tosca_service');

  require('scripts/topology/services/topology_layout_services');
  require('scripts/topology/services/connector_drag_service');

  modules.get('a4c-topology-editor', ['a4c-tosca', 'a4c-common-graph']).factory('topologySvgFactory', ['svgServiceFactory', 'topologyLayoutService', 'routerFactoryService', 'toscaService', 'd3Service', 'relationshipMatchingService', 'connectorDragFactoryService',
    function(svgServiceFactory, topologyLayoutService, routerFactoryService, toscaService, d3Service, relationshipMatchingService, connectorDragFactoryService) {
      function TopologySvg (callbacks, containerElement, isRuntime, nodeRenderer) {
        this.isGridDisplayed = false;
        this.firstRender = true;

        this.setNodeRenderer(nodeRenderer);

        this.clickCallback = callbacks.click;
        this.addRelationship = callbacks.addRelationship;

        this.isRuntime = isRuntime;

        this.selectedNodeId = null;
        // create svg element
        this.svgGraph = svgServiceFactory.create(containerElement, 'topologySvgContainer', 'topology-svg');
        this.svg = this.svgGraph.svg;
        d3.selectAll('.d3-tip').remove();
        var self = this;
        this.tip = d3Tip().attr('class', 'd3-tip').html(function(element) {
          return self.tooltip(element);
        });
        this.svg.call(this.tip);

        // capabilities drag and drop manager
        this.connectorDrag = connectorDragFactoryService.create(this);
      }

      TopologySvg.prototype = {
        constructor: TopologySvg,

        onResize: function(dimensions) {
          this.svgGraph.onResize(dimensions.width, dimensions.height);
          this.svgGraph.controls.coordinateUtils.reset();
          this.svgGraph.controls.updateViewBox();
        },

        checkBrowser: function(browserName) {
          return navigator.userAgent.indexOf(browserName) !== -1;
        },

        setNodeRenderer: function(nodeRenderer) {
          if(this.nodeRenderer === nodeRenderer) {
            return;
          }
          if(_.defined(this.svg)) {
            this.svg.selectAll('.node-template').remove();
          }
          this.nodeRenderer = nodeRenderer;
          this.gridStep = 10;
          this.reset(this.topology);
        },

        reset: function(topology) {
          var i;

          this.topology = topology;

          if (!_.defined(this.topology)) {
            return;
          }

          // Compute the automatic layout for the topology.
          var nodeRenderer = this.nodeRenderer;

          this.layout = topologyLayoutService.layout(this.topology.topology.nodeTemplates, this.topology, this.nodeRenderer);

          // Update connector routing.
          this.grid = routerFactoryService.create(this.layout.bbox, this.gridStep);
          for(i = 0; i< this.layout.nodes.length;i++) {
            this.grid.addObstacle(this.layout.nodes[i].bbox);
          }
          for(i = 0; i< this.layout.links.length;i++) {
            this.computeLinkRoute(this.layout.links[i]);
          }

          // draw the svg
          this.draw(this.layout);
          this.displayGrid();

          this.svgGraph.controls.coordinateUtils.bbox = this.layout.bbox;
          this.svgGraph.controls.coordinateUtils.reset();
          // this.svgGraph.controls.updateViewBox();
        },

        computeLinkRoute: function(link) {
          // compute the route path
          var route;
          if(link.isNetwork) {
            route = [];
          } else {
            route = this.grid.route(link.source, link.source.direction, link.target, link.target.direction);
          }
          link.route = route;
        },

        displayGrid: function() {
          if(!this.isGridDisplayed) {
            return;
          }
          for (var i = 0; i < this.grid.cells.length; i++) {
            for (var j = 0; j < this.grid.cells[i].length; j++) {
              var rect = d3Service.rect(this.svg, this.grid.bbox.minX + i * this.gridStep, this.grid.bbox.minY + j * this.gridStep, this.gridStep - 1, this.gridStep - 1, 0, 0, null).style('fill-opacity', 0.3);
              if (this.grid.cells[i][j].visited === 1) {
                rect.style('fill', 'green');
              } else if (this.grid.cells[i][j].weight === 0) {
                rect.style('fill', 'blue');
              } else {
                rect.style('fill', 'red');
              }
            }
          }
        },

        draw: function(layout) {
          var self = this;

          var nodes = layout.nodes;
          var links = layout.links;

          // remove the group bullets
          var nodeGroupSelection = this.svg.selectAll('.node-template-group');
          nodeGroupSelection.remove();

          var nodeSelection = this.svg.selectAll('.node-template').data(nodes, function(node) { return node.id; });

          // update existing nodes
          nodeSelection.each(function(node) {
            var nodeGroup = d3.select(this);
            self.updateNode(nodeGroup, node);
          });

          // create new nodes
          var newNodeGroups = nodeSelection.enter().append('g').attr('class', 'node-template');
          newNodeGroups.each(function(node) {
            var nodeGroup = d3.select(this);
            self.createNode(nodeGroup, node);
            self.updateNode(nodeGroup, node);
          });

          // remove destroyed nodes.
          nodeSelection.exit().remove();

          this.drawLink(this.svg, links);
        },

        createNode: function(nodeGroup, node) {
          var nodeTemplate = node.template;
          var nodeType = this.topology.nodeTypes[nodeTemplate.type];

          var self = this;
          var actions = {
            click: function() {
              // un-select last node and select the new one on click
              self.clickCallback({
                'newSelectedName': node.id,
                'oldSelectedName': self.selectedNodeId
              });

              self.selectedNodeId = node.id;
            },
            mouseover: this.tip.show,
            mouseout: this.tip.hide,
            connectorDrag: this.connectorDrag
          };

          this.nodeRenderer.createNode(this.layout, nodeGroup, node, nodeTemplate, nodeType, this.topology, actions);
        },

        updateNode: function(nodeGroup, node) {
          var nodeTemplate = node.template;
          var nodeType = this.topology.nodeTypes[nodeTemplate.type];

          // update location
          nodeGroup.attr('transform', function(d) {
            return 'translate(' + d.coordinate.x + ',' + d.coordinate.y + ')';
          });
          // update background class
          nodeGroup.classed('selected', function(){ return nodeTemplate.selected; });

          this.nodeRenderer.updateNode(nodeGroup, node, nodeTemplate, nodeType, this.topology);

          var scalingPolicySelection = null;
          var scalingPolicy = toscaService.getScalingPolicy(nodeTemplate);

          if (toscaService.isOneOfType(['tosca.nodes.Network'], nodeTemplate.type, this.topology.nodeTypes)) {
            var netX = node.bbox.width();
            var netMaxX = this.layout.bbox.width();
            var netY = node.bbox.height() / 2 - 2;
            var path = 'M '+netX+','+netY+' '+netMaxX+','+netY;
            nodeGroup.select('.link-network').attr('d', path);
          }

          if(_.defined(scalingPolicy)) {
            scalingPolicySelection = nodeGroup.select('#scalingPolicy');
            if(scalingPolicySelection.empty()) {
              var scalingPolicyGroup = nodeGroup.append('g').attr('id', 'scalingPolicy');
              scalingPolicyGroup.append('circle').attr('cx', oX + this.nodeRenderer.width).attr('cy', oY).attr('r', '12').attr('class', 'topology-svg-icon-circle');
              scalingPolicyGroup.append('text').attr('class', 'topology-svg-icon topology-svg-icon-center').attr('x', oX + this.nodeRenderer.width).attr('y', oY)
                .attr('transform', 'rotate(90 '+(oX + this.nodeRenderer.width)+' '+oY+')')
                .text(function() { return '\uf112'; });
            }
          } else {
            scalingPolicySelection = nodeGroup.select('#scalingPolicy');
            if(!scalingPolicySelection.empty()) {
              scalingPolicySelection.remove();
            }
          }
        },

        tooltip: function (element) {
          return this.nodeRenderer.tooltip(element);
        },

        drawLink: function(parent, links) {
          var self = this;

          var topology = this.topology;

          var linkSelection = parent.selectAll('.link').data(links, function(link) { return link.id; });

          linkSelection.each(function() {
            var linkPath = d3.select(this);
            self.drawLinkPath(linkPath, false);
          });

          var newLinks = linkSelection.enter().append('g');
          newLinks.each(function(link) {
            var linkPath = d3.select(this);
            if(link.isNetwork) {
              var netStyle = link.networkId % self.networkStyles;
              linkPath.attr('class', 'link link-network link-network-' + netStyle);
            } else {
              linkPath.attr('class', 'link');
              var isHostedOn = toscaService.isHostedOnType(link.type, topology.relationshipTypes);
              linkPath.classed('link-hosted-on', function() { return isHostedOn; })
                .classed('link-depends-on', function() { return !isHostedOn; });
            }
            self.drawLinkPath(linkPath, true);
          });

          linkSelection.exit().remove();
        },

        drawLinkPath: function(linkPath, create) {
          var line = d3.svg.line()
            .x(function(d) { return d.x; })
            .y(function(d) { return d.y; })
            .interpolate("basis");
          var path;
          if(create) {
            path = linkPath.append('path');
          } else {
            path = linkPath.select('path');
          }
          path.attr('d', function(d){ return line(d.route)});
          // linkPath.attr('d', function(link) {
          //     // compute the route path
          //     var route = link.route;
          //     var path = 'M' + route[0].x + ',' + route[0].y;
          //     for (var i = 1; i < route.length - 1; i++) {
          //       path = path + ' ' + route[i].x + ',' + route[i].y;
          //     }
          //     path = path + ' ' + route[route.length - 1].x + ',' + route[route.length - 1].y;
          //
          //     return path;
          //   });
          linkPath.classed('link-selected', function(link) { return link.selected; });
        }
      };

      return {
        create: function(clickCallback, containerElement, isRuntime, nodeRenderer) {
          return new TopologySvg(clickCallback, containerElement, isRuntime, nodeRenderer);
        }
      };
    } // function
  ]); // factory
}); // define
