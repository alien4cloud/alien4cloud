// Directive allowing to display a workflow (tosca plan)
define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var d3Tip = require('d3-tip');
  var d3 = require('d3');
  require('dagre');
  require('graphlib');
  var dagreD3 = require('dagre-d3');
  require('scripts/common-graph/services/runtime_color_service');
  require('scripts/common-graph/services/svg_service');

  modules
      .get('a4c-topology-editor',
          [ 'a4c-common', 'ui.bootstrap', 'a4c-styles' ])
      .directive(
          'topologyPlan',
          [
              '$filter',
              '$http',
              '$modal',
              '$interval',
              '$translate',
              'svgServiceFactory',
              'runtimeColorsService',
              'listToMapService',
              'bboxFactory',
              function($filter, $http, $modal, $interval, $translate, svgServiceFactory,
                  runtimeColorsService, listToMapService, bboxFactory) {
                return {
                  restrict : 'E',
//                  scope : {
//                    workflows : '=',
//                    topology : '=',
//                    dimensions : '='
//                  },
                  link : function(scope) {
                    // Default parent svg markup to render the workflow
                    var containerElement = d3.select('#plan-graph-container');

                    var svgGraph = svgServiceFactory.create(containerElement,
                        'plan-svg', 'plan-svg');

                    // Create the renderer
                    var render = new dagreD3.render();

                    // Create the input graph
                    var g = new dagreD3.graphlib.Graph({
                      compound : true
                    }).setGraph({}).setDefaultEdgeLabel(function() {
                      return {};
                    });
                    // we want horizontal layout
                    g.graph().rankdir = "LR";
                    g.graph().ranksep = 20;
                    if (scope.wfViewMode === 'simple') {
                      g.graph().ranksep = 5;
                    }

                    var oldDrawEdges = render.createEdgePaths();
                    render.createEdgePaths(function(selection, g, arrows) {
                        var edges = oldDrawEdges(selection, g, arrows);
                        edges.on('click', function (edge) { 
                          // select the link in order to allow actions on it (ie. remove)
                          if (edge.v !== 'start' && edge.w !== 'end') {
                            // edge connected to start or end are no editable
                            scope.workflows.togglePinEdge(edge.v, edge.w);
                          }
                        });
                        return edges;
                    });
                    
                    // Add our custom shapes
                    render.shapes().start = function(parent, bbox, node) {
                      var r = bbox.height / 2;
                      var shapeSvg = parent.insert("circle", ":first-child").attr('cx', '0').attr('cy', '0').attr('r', r).attr('style', 'fill:none; stroke:green; stroke-width:3');
                      node.intersect = function(point) {
                        return dagreD3.intersect.rect(node, point);
                      };
                      return shapeSvg;
                    };
                    render.shapes().stop = function(parent, bbox, node) {
                      var r = bbox.height / 2;
                      var r2 = r * 0.6;
                      var shapeSvg = parent.insert('circle').attr('cx', '0').attr('cy', '0').attr('r', r).attr('style', 'fill:none; stroke:'+runtimeColorsService.started+'; stroke-width:2');
                      shapeSvg = parent.append('circle').attr('cx', '0').attr('cy', '0').attr('r', r2).attr('style', 'fill:'+runtimeColorsService.started+'; stroke:'+runtimeColorsService.started+'; stroke-width:1');
                      node.intersect = function(point) {
                        return dagreD3.intersect.rect(node, point);
                      };
                      return shapeSvg;
                    };
                    render.shapes().operationStep = function(parent, bbox, node) {
                      var nodeId = node.elem.__data__;
                      var step = steps[nodeId];
                      var nodeName = step.nodeId;
                      var nodeType = undefined;
                      if (scope.topology.topology.nodeTemplates[nodeName]) {
                        var typeName = scope.topology.topology.nodeTemplates[nodeName].type;
                        nodeType = scope.topology.nodeTypes[typeName];
                      }
                      var x = (bbox.width / 2) * -1;
                      var w = bbox.width;
                      var y = (bbox.height / 2) * -1;
                      var h = bbox.height;
                      
                      var shortActivityType = scope.workflows.getStepActivityType(step);
                      var simpleView = (scope.wfViewMode === 'simple' && shortActivityType === 'SetStateActivity');
                      var shapeSvg = parent.insert('rect').attr('x', x).attr('y', y).attr('width', w).attr('height', h).attr('rx', 5).attr('ry', 5).style("fill", "white");
                      if (errorRenderingData.errorSteps[nodeId]) {
                        // the step is in a bad sequence, make it red
                        shapeSvg.style("stroke", "#f66");
                      } else {
                        if (scope.wfViewMode === 'simple'){
                          shapeSvg.style("stroke", "DarkGray");
                        } else {
                          shapeSvg.style("stroke", "grey");
                        }
                      }
                      var iconSize = 25;
//                      var html = parent.append('foreignObject').attr('x', x + w- 20).attr('y', y + 2).attr('width', 18).attr('height', 18);
                      var icon = undefined;
                      if (simpleView){
                        icon = parent.append('text').attr('class', 'fa').attr('x', x + 8).attr('y', y + 17).text(scope.workflows.getStepActivityTypeIcon(step));
                      } else {
                        icon = parent.append('text').attr('class', 'fa').attr('x', x + w - 22).attr('y', y + 16).text(scope.workflows.getStepActivityTypeIcon(step));
                      }
                      if (shortActivityType === 'OperationCallActivity') {
                        parent.append('text').attr('class', 'wfOperationLabel').attr('y', y + h - 10).text(_.trunc(step.activity.operationName, {'length': 10})).style("text-anchor", "middle");
                      } else if (shortActivityType === 'DelegateWorkflowActivity') {
                        parent.append('text').attr('class', 'wfDelegateLabel').attr('fill', '#7A7A52').attr('y', y + h - 10).text(_.trunc(step.activity.workflowName, {'length': 10})).style("text-anchor", "middle");
                      } else if (shortActivityType === 'SetStateActivity' && !simpleView) {
                        parent.append('text').attr('class', 'wfStateLabel').attr('fill', '#003399').attr('y', y + h - 8).text(_.trunc(step.activity.stateName, {'length': 13})).style("text-anchor", "middle");
                        iconSize = 16;
                      }
                      if (nodeType && nodeType.tags && !simpleView) {
                        var tags = listToMapService.listToMap(nodeType.tags, 'name', 'value');
                        if (tags.icon) {
                          parent.append('image').attr('x', x + 5).attr('y', y + 5).attr('width', iconSize).attr('height', iconSize).attr('xlink:href',
                            'img?id=' + tags.icon + '&quality=QUALITY_32');
                        }
                      }
                      if (scope.workflows.isStepPinned(nodeId)) {
                        shapeSvg.style("fill", "#CCE0FF");
                      } else if (scope.workflows.isStepSelected(nodeId)) {
                        shapeSvg.style("fill", "#FFFFD6");
                      }

                      node.intersect = function(point) {
                        return dagreD3.intersect.rect(node, point);
                      };
                      var onMouseOver = function(d) {
                        scope.workflows.previewStep(steps[nodeId]);
                      };
                      var onMouseOut = function(d) {
                        scope.workflows.exitPreviewStep();
                      }
                      var onClick = function(d) {
                        var stepPinned = scope.workflows.isStepPinned(nodeId);
                        var stepSelected = scope.workflows.isStepSelected(nodeId);
                        var hasStepPinned = scope.workflows.hasStepPinned();
                        if (stepPinned) {
                          // the step is pinned, let's unpin it
                          scope.workflows.togglePinnedworkflowStep(nodeId, steps[nodeId]);
                        } else if(hasStepPinned) {
                          // a step is pinned, we play with selections
                          scope.workflows.toggleStepSelection(nodeId);
                        } else if(stepSelected) {
                          // the step is selected
                          scope.workflows.toggleStepSelection(nodeId);
                        } else {
                          // no step pinned, let's pin this one
                          scope.workflows.togglePinnedworkflowStep(nodeId, steps[nodeId]);
                        }
                      };
                      //
                      shapeSvg.on("mouseover", onMouseOver);
                      shapeSvg.on("mouseout", onMouseOut);                      
                      shapeSvg.on("click", onClick);
                      // in simple view mode, we want to be able to click on icons
                      icon.on("mouseover", onMouseOver);
                      icon.on("mouseout", onMouseOut);                        
                      icon.on("click", onClick);
                      return shapeSvg;
                    };
                    
                    // Set up an SVG group so that we can translate the final
                    // graph.
                    var svg = d3.select('#plan-svg'), svgGroup = svg
                        .append('g');

                    // Set up zoom support
                    var zoom = d3.behavior.zoom().on(
                        "zoom",
                        function() {
                          svgGroup.attr("transform", "translate("
                              + d3.event.translate + ")" + "scale("
                              + d3.event.scale + ")");
                        });
                    svg.call(zoom);

                    var initialScale = 1;
//                    svg.attr('width', 1200);
//                    svg.attr('height', 800);
                    zoom.scale(initialScale).event(svg);

                    // the hosts (graph clusters)
                    var hosts = [];
                    // the steps
                    var steps = [];
                    // data used to render errors
                    var errorRenderingData = {cycles: {}, errorSteps: {}};

                    function setCurrentWorkflowStep(nodeId) {
                      scope.workflows.setCurrentworkflowStep(nodeId, steps[nodeId]);
                    }

                    function appendStepNode(g, stepName, step) {
                      var shortActivityType = scope.workflows.getStepActivityType(step);
                      if (shortActivityType === 'OperationCallActivity') {
                        g.setNode(stepName, {label: '', width: 60, height: 40, shape: 'operationStep'});
                      } else if (shortActivityType === 'DelegateWorkflowActivity') {
                        g.setNode(stepName, {label: '', width: 60, height: 40, shape: 'operationStep'});
                      } else if (shortActivityType === 'SetStateActivity') {
                        if (scope.wfViewMode === 'simple') {
                          g.setNode(stepName, {label: '', width: 12, height: 4, shape: 'operationStep'});
                        } else {
                          g.setNode(stepName, {label: '', width: 40, height: 25, shape: 'operationStep'});
                        }
                      } else {
                        g.setNode(stepName, {label: step.name});
                      }
                    }
                    
                    function appendEdge(g, from, to) {
                      var stokeWidth = '1.5px';
                      if (scope.workflows.isEdgePinned(from, to)) {
                        stokeWidth = '5px';
                      }
                      var style = {
                          lineInterpolate: 'basis', 
                          arrowhead: 'vee', 
                          style: "stroke: black; stroke-width: " + stokeWidth + ";", 
                          arrowheadStyle: "fill: black; stroke: black"};
                      if (errorRenderingData.cycles[from] && _.contains(errorRenderingData.cycles[from], to)) {
                        // the edge is in a cycle, make it red
                        style = {
                            lineInterpolate: 'basis',
                            arrowhead: 'vee',
                            style: "stroke: #f66; stroke-width: " + stokeWidth + ";",
                            arrowheadStyle: "fill: #f66; stroke: #f66"}
                      }
                      g.setEdge(from, to, style);
                    }

                    function refresh() {
                      console.log("refresh");
                      // remove remaining popups
                      d3.selectAll('.d3-tip').remove();
                      g.nodes().forEach(function(v) {
                        g.removeNode(v);
                      });

                      if (!scope.currentWorkflowName || !scope.topology.topology.workflows || !scope.topology.topology.workflows[scope.currentWorkflowName]) {
                        render(svgGroup, g);
                        return;
                      }
                      errorRenderingData = scope.workflows.getErrorRenderingData();
                      
                      hosts = scope.topology.topology.workflows[scope.currentWorkflowName].hosts;
                      steps = scope.topology.topology.workflows[scope.currentWorkflowName].steps;

                      g.setNode("start", {label : '', shape: 'start'});
                      g.setNode("end", {label : '', shape: 'stop'});
                      if (hosts) {
                        for (var hostIdx = 0; hostIdx < hosts.length; hostIdx++) {
                          var host = hosts[hostIdx];
                          g.setNode(host, {label : host, clusterLabelPos : 'top'});
                        }
                      }
                      var hasSteps = false;
                      if (steps) {
                        for (var stepName in steps) {
                          hasSteps = true;
                          var step = steps[stepName];
                          appendStepNode(g, stepName, step);
                          if (step.hostId) {
                            g.setParent(stepName, step.hostId);
                          }
                        }
                        for (var stepName in steps) {
                          var step = steps[stepName];
                          if (!step.precedingSteps || step.precedingSteps.length == 0) {
                            appendEdge(g, "start", stepName);
                          }
                          if (!step.followingSteps || step.followingSteps.length == 0) {
                            appendEdge(g, stepName, "end");
                          } else {
                            for (var i = 0; i < step.followingSteps.length; i++) {
                              appendEdge(g, stepName, step.followingSteps[i]);
                            }
                          }
                        }
                      }
                      if (!hasSteps) {
                        appendEdge(g, "start", "end");
                      }

                      // Run the renderer. This is what draws the final graph.
                      render(svgGroup, g);

                      // tool tips
                      var tip = d3Tip().attr('class', 'd3-tip wf-tip').offset([-10, 0]).html(function(d) { return styleTooltip(d); });
                      svg.call(tip);
                      d3.selectAll("g.node").on('mouseover', tip.show).on('mouseout', tip.hide);

                    }

                    // render an styled html tool tip for a given step
                    var styleTooltip = function(nodeId) {
                      var step = steps[nodeId];
                      if (!step) {
                        return nodeId;
                      }
                      var html = '<div>';
                      html += '<h5 class="pull-left">' + step.name + '</h5>';
                      html += '<i class="fa pull-right">' + scope.workflows.getStepActivityTypeIcon(step) + '</i>';
                      html += '<span class="clearfix"></span>';
                      html += '<div class="row"><div class="col-md-3">Node' + ': </div><div class="col-md-9"><b>' + step.nodeId + '</b></div></div>';
                      html += '<div class="row"><div class="col-md-3">Host' + ': </div><div class="col-md-9"><b>' + step.hostId + '</b></div></div>';
                      html += '<div class="row"><div class="col-md-3">' + $translate('APPLICATIONS.WF.activity') + ': </div>';
                      html += '<div class="col-md-9"><b>' + $translate('APPLICATIONS.WF.' + scope.workflows.getStepActivityType(step)) + '</b></div></div>';
                      var activityDetails = scope.workflows.getStepActivityDetails(step);
                      for (var propName in activityDetails) {
                        html += '<div class="row"><div class="col-md-3">';
                        html += $translate('APPLICATIONS.WF.' + propName) + ': </div><div class="col-md-9 wfActivityDetail"><b>' + _.startTrunc(activityDetails[propName], 25) + '</b></div></div>';
                      }
                      html += '</div>';
                      return html;
                    };


                    scope.$on('WfRefresh', function (event) {
                      refresh();
                    });
                    scope.$watch('visualDimensions', function(visualDimensions) {
                      onResize(visualDimensions.width, visualDimensions.height);
                    });

                    scope.$watch('topology', function() {
                      scope.workflows.topologyChanged();
                    }, true);

                    function onResize(width, height) {
                      svgGraph.onResize(width, height);
                    }

                    scope.$watch('visualDimensions', function(visualDimensions) {
                      onResize(visualDimensions.width, visualDimensions.height);
                    });
                    svgGraph.onResize(scope.visualDimensions.width,
                        scope.visualDimensions.height);

                    function centerGraph() {
                      // Center the graph
//                      var xCenterOffset = (svg.attr("width") - g.graph().width) / 2;
//                      svgGroup.attr("transform", "translate(" + xCenterOffset + ", 20)");
//                      svg.attr("height", g.graph().height + 40);
                    }

                    // preview events registering
                    function setPreviewEdge(g, from , to) {
                      g.setEdge(from, to, {
                        lineInterpolate: 'basis',
                        style: "stroke: blue; stroke-width: 3px; stroke-dasharray: 5, 5;",
                        arrowheadStyle: "fill: blue; stroke: blue"
                      });
                    }
                    function setPreviewNode(g) {
                      g.setNode('a4cPreviewNewStep', {label : '?', style: "stroke: blue", labelStyle: "fill: blue; font-weight: bold; font-size: 2em"});
                    }
                    scope.$on('WfCenterGraph', function (event) {
                      centerGraph();
                    });
                    scope.$on('WfRemoveEdgePreview', function (event, from, to) {
                      console.log("WfRemoveEdgePreview event received : " + event + ", from:" + from + ", to:" + to);
                      g.removeEdge(from, to);
                      if (steps[from].followingSteps.length == 1) {
                        setPreviewEdge(g, from, 'end');
                      }
                      if (steps[to].precedingSteps.length == 1) {
                        setPreviewEdge(g, 'start', to);
                      }
                      render(svgGroup, g);
                    });
                    scope.$on('WfResetPreview', function (event) {
                      console.log("WfResetPreview event received : " + event);
                      refresh();
                    });
                    scope.$on('WfConnectPreview', function (event, from, to) {
                      console.log("WfConnectPreview event received : " + event + ", from:" + from + ", to:" + to);
                      for (var i = 0; i < from.length; i++) {
                        g.removeEdge(from[i], 'end');
                        for (var j = 0; j < to.length; j++) {
                          g.removeEdge('start', to[j]);
                          setPreviewEdge(g, from[i], to[j]);
                        }
                      }
                      render(svgGroup, g);
                    });
                    scope.$on('WfAddStepPreview', function (event) {
                      setPreviewNode(g);
                      setPreviewEdge(g, 'start', 'a4cPreviewNewStep');
                      setPreviewEdge(g, 'a4cPreviewNewStep', 'end');
                      if (_.size(steps) == 0) {
                        g.removeEdge('start', 'end');
                      }
                      render(svgGroup, g);
                    });
                    scope.$on('WfInsertStepPreview', function (event, stepId) {
                      console.log("WfInsertStepPreview event received : " + event + ", stepId:" + stepId);
                      setPreviewNode(g);
                      var precedingStep = undefined;
                      if (steps[stepId].precedingSteps.length == 0) {
                        precedingStep = 'start';
                      } else if (steps[stepId].precedingSteps.length == 1) {
                        precedingStep = steps[stepId].precedingSteps[0];
                      }
                      if (precedingStep) {
                        g.removeEdge(precedingStep, stepId);
                        setPreviewEdge(g, precedingStep, 'a4cPreviewNewStep');
                      }
                      setPreviewEdge(g, 'a4cPreviewNewStep', stepId);
                      render(svgGroup, g);
                    });
                    scope.$on('WfAppendStepPreview', function (event, stepId) {
                      console.log("WfAppendStepPreview event received : " + event + ", stepId:" + stepId);
                      setPreviewNode(g);
                      var followingStep = undefined;
                      if (steps[stepId].followingSteps.length == 0) {
                        followingStep = 'end';
                      } else if (steps[stepId].followingSteps.length == 1) {
                        followingStep = steps[stepId].followingSteps[0];
                      }
                      if (followingStep) {
                        g.removeEdge(stepId, followingStep);
                        setPreviewEdge(g, 'a4cPreviewNewStep', followingStep);
                      }
                      setPreviewEdge(g, stepId, 'a4cPreviewNewStep');
                      render(svgGroup, g);
                    });
                    scope.$on('WfRemoveStepPreview', function (event, stepId) {
                      console.log("WfRemoveStepPreview event received : " + event + ", stepId:" + stepId);
                      g.removeNode(stepId);
                      var precedingSteps = undefined;
                      if (!steps[stepId].precedingSteps || steps[stepId].precedingSteps.length == 0) {
                        precedingSteps = ['start'];
                      } else if (steps[stepId].followingSteps) {
                        precedingSteps = steps[stepId].precedingSteps;
                      }
                      var followingSteps = undefined;
                      if (!steps[stepId].followingSteps || steps[stepId].followingSteps.length == 0) {
                        followingSteps = ['end'];
                      } else if (steps[stepId].followingSteps) {
                        followingSteps = steps[stepId].followingSteps;
                      }
                      for (var i = 0; i < precedingSteps.length; i++) {
                        for (var j = 0; j < followingSteps.length; j++) {
                          if (precedingSteps[i] === 'start' && followingSteps[j] === 'end') {
                            continue;
                          }
                          setPreviewEdge(g, precedingSteps[i], followingSteps[j]);
                        }
                      }
                      render(svgGroup, g);
                    });
                    function swapLinks(from, to) {
                      // from's preceding become preceding of to
                      var precedingSteps = undefined;
                      if (!steps[from].precedingSteps || steps[from].precedingSteps.length == 0) {
                        precedingSteps = ['start'];
                      } else {
                        precedingSteps = steps[from].precedingSteps;
                      }
                      for (var i = 0; i < precedingSteps.length; i++) {
                        g.removeEdge(precedingSteps[i], from);
                        if (precedingSteps[i] !== to) {
                          setPreviewEdge(g, precedingSteps[i], to);
                        }
                      }
                      // from's following become following of 'to' (except 'to' itself)
                      var followingSteps = undefined;
                      if (!steps[from].followingSteps || steps[from].followingSteps.length == 0) {
                        followingSteps = ['end'];
                      } else {
                        followingSteps = steps[from].followingSteps;
                      }
                      for (var i = 0; i < followingSteps.length; i++) {
                        g.removeEdge(from, followingSteps[i]);
                        if (followingSteps[i] !== to) {
                          setPreviewEdge(g, to, followingSteps[i]);
                        }
                      }                      
                    }
                    // swap steps : connections between both is inversed and each other connections are swapped
                    scope.$on('WfSwapPreview', function (event, from, to) {
                      g.removeEdge(from, to);
                      swapLinks(from, to);
                      swapLinks(to, from);
                      setPreviewEdge(g, to, from);
                      render(svgGroup, g);
                    });
                  }
                };
              } ]); // factory
}); // define
