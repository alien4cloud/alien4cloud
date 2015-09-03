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
                      var typeName = scope.topology.topology.nodeTemplates[nodeName].type;
                      var nodeType = scope.topology.nodeTypes[typeName];
                      var x = (bbox.width / 2) * -1;
                      var w = bbox.width;
                      var y = (bbox.height / 2) * -1;
                      var h = bbox.height;
                      var shapeSvg = parent.insert('rect').attr('x', x).attr('y', y).attr('width', w).attr('height', h).attr('rx', 5).attr('ry', 5).style("fill", "white").style("stroke", "grey");
                      var iconSize = 20;
//                      var html = parent.append('foreignObject').attr('x', x + w- 20).attr('y', y + 2).attr('width', 18).attr('height', 18);
                      parent.append('text').attr('class', 'fa').attr('x', x + w- 20).attr('y', y + 16).text(scope.workflows.getStepActivityTypeIcon(step)); 
                      if (step.activity.type === 'alien4cloud.paas.wf.OperationCallActivity') {
//                        html.append("xhtml:body").attr('class', 'svgHtml').html('<i class="fa fa-cogs"></i>');
                        parent.append('text').attr('class', 'wfOperationLabel').attr('y', y + h - 10).text(_.trunc(step.activity.operationName, {'length': 13})).style("text-anchor", "middle");
                      } else if (step.activity.type === 'alien4cloud.paas.wf.SetStateActivity') {
//                        html.append("xhtml:body").attr('class', 'svgHtml').html('<i class="fa fa-wifi"></i>');
                        parent.append('text').attr('class', 'wfStateLabel').attr('fill', '#003399').attr('y', y + h - 8).text(_.trunc(step.activity.stateName, {'length': 13})).style("text-anchor", "middle");
                        iconSize = 16;
                      }  
                      if (nodeType.tags) {
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
                      shapeSvg.on("mouseover", function(d) {
                        scope.workflows.previewStep(steps[nodeId]);
                      });
                      shapeSvg.on("mouseout", function(d) {
                        scope.workflows.exitPreviewStep();
                      });                      
                      //
                      shapeSvg.on("click", function(d) {
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
//                        $interval(function() {
//                          refresh();
//                        }, 0, 1);                        
                      });                      
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
                    
//                    var oldcreateNodes = render.createNodes();
//                    render.createNodes(function(selection, g, shapes) {
//                      var svgNodes = oldcreateNodes(selection, g, shapes);
//                      svgNodes.each(function(nodeId) {
////                        console.log(v);
//                        var node = g.node(nodeId);
//                        var thisGroup = d3.select(this);
////                        thisGroup.attr("title", 'kikooo ' + nodeId);
//                        if (scope.workflows.isStepPinned(nodeId)) {
//                          thisGroup.attr("fill", 'green');
//                        } else if (scope.workflows.isStepSelected(nodeId)) {
//                          thisGroup.attr("fill", 'red');
//                        } else {
//                          thisGroup.attr("fill", 'black');
//                        }
//                        thisGroup.on("click", function(nodeId) {
//                          if (scope.workflows.isStepPinned(nodeId)) {
//                            return;
//                          }
//                          if (scope.workflows.toggleStepSelection(nodeId)) {
//                            thisGroup.attr("fill", 'red');
//                          } else {
//                            thisGroup.attr("fill", 'black');
//                          }
//                        });
//                        thisGroup.on("mouseover", function(nodeId) {
//                          scope.workflows.setCurrentworkflowStep(nodeId, steps[nodeId]);
//                        });
//                        thisGroup.on("contextmenu", function(nodeId) {
//                          // stop showing browser menu
//                          d3.event.preventDefault();
//                          if (scope.workflows.togglePinnedworkflowStep(nodeId, steps[nodeId])) {
//                            thisGroup.attr("fill", 'green');
//                          } else if (scope.workflows.isStepSelected(nodeId)) {
//                            thisGroup.attr("fill", 'red');
//                          } else {
//                            thisGroup.attr("fill", 'black');
//                          }
//                        });                        
//                      });
//                      return svgNodes;
//                    });
                    
                    // the hosts (graph clusters)
                    var hosts = [];
                    // the steps
                    var steps = [];
                    
                    function setCurrentWorkflowStep(nodeId) {
                      scope.workflows.setCurrentworkflowStep(nodeId, steps[nodeId]);
                    }
                    
                    function appendStepNode(g, stepName, step) {
                      if (step.activity.type === 'alien4cloud.paas.wf.OperationCallActivity') {
                        g.setNode(stepName, {label: '', width: 60, height: 40, shape: 'operationStep'});
                      } else if (step.activity.type === 'alien4cloud.paas.wf.SetStateActivity') {
                        g.setNode(stepName, {label: '', width: 40, height: 25, shape: 'operationStep'});
                      } else {
                        g.setNode(stepName, {label: step.name});
                      }                      
                    }
                    
                    function refresh() {
                      console.log("refresh");
                      d3.selectAll('.d3-tip').remove();
                      g.nodes().forEach(function(v) {
                        g.removeNode(v);
                      });
                      
                      if (!scope.currentWorkflowName || !scope.topology.topology.workflows || !scope.topology.topology.workflows[scope.currentWorkflowName]) {
                        render(svgGroup, g);
                        return;
                      }
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
//                          g.setNode(stepName, {label: '', width: 60, height: 40, shape: 'operationStep'});
                          if (step.hostId) {
                            g.setParent(stepName, step.hostId);
                          }
                        }
                        for (var stepName in steps) {
                          var step = steps[stepName];
                          if (!step.precedingSteps || step.precedingSteps.length == 0) {
                            g.setEdge("start", stepName);
                          }
                          if (!step.followingSteps || step.followingSteps.length == 0) {
                            g.setEdge(stepName, "end");
                          } else {
                            for (var i = 0; i < step.followingSteps.length; i++) {
                              g.setEdge(stepName, step.followingSteps[i]);
                            }
                          }
                        }
                      } 
                      if (!hasSteps) {
                        g.setEdge("start", "end");
                      }

                      g.nodes().forEach(function(v) {
                        var node = g.node(v);
                        // Round the corners of the nodes
                        node.rx = node.ry = 5;
                      });

                      //makes the lines smooth
                      g.edges().forEach(function (e) {
                          var edge = g.edge(e.v, e.w);
                          edge.lineInterpolate = 'basis';
                          edge.label = '';
                          edge.arrowhead = 'vee'; // doesn't work!
                          edge.arrowheadStyle= "fill: black";
                          edge.style = {};
                      });
                      
                      // Run the renderer. This is what draws the final graph.
                      render(svgGroup, g);
                      
                      // tool tips
                      var tip = d3Tip().attr('class', 'd3-tip wf-tip').offset([-10, 0]).html(function(d) { return styleTooltip(d); });
                      svg.call(tip);
//                      d3.selectAll("g.node").call(tip);
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
                  }
                };
              } ]); // factory
}); // define
