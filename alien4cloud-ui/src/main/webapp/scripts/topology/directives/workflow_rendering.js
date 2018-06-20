// Directive allowing to display a workflow (tosca plan)
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var d3Tip = require('d3-tip');
  var d3 = require('d3');

  require('scripts/common-graph/services/runtime_color_service');
  require('scripts/common-graph/services/svg_service');
  require('scripts/topology/services/workflow_shapes');
  require('scripts/topology/services/workflow_render');

  modules.get('a4c-topology-editor', ['a4c-common', 'a4c-common-graph', 'ui.bootstrap', 'a4c-styles']).directive('topologyPlan',
    ['$filter', '$http', '$uibModal', '$interval', '$translate', 'svgServiceFactory', 'runtimeColorsService', 'listToMapService',
      'workflowShapes', 'planRender',
      function ($filter, $http, $uibModal, $interval, $translate, svgServiceFactory,
                runtimeColorsService, listToMapService, workflowShapes, planRender) {
        return {
          restrict: 'E',
          link: function (scope) {
            // Default parent svg markup to render the workflow
            var containerElement = d3.select('#plan-graph-container');
            var contextContainer = d3.select('#editor-context-container');
            contextContainer.html('');
            var svgGraph = svgServiceFactory.create(containerElement, 'plan-svg', 'plan-svg', contextContainer);
            var svgGroup = svgGraph.svgGroup;
            // add markers for arrows
            workflowShapes.initMarkers(svgGraph.svg);

            scope.$watch('triggerRefresh', function () {
              scope.workflows.topologyChanged();
            });

            scope.$watch('visualDimensions', function (visualDimensions) {
              onResize(visualDimensions.width, visualDimensions.height);
            });

            function onResize(width, height) {
              svgGraph.onResize(width, height);
            }

            function centerGraph() {
              svgGraph.controls.reset();
            }

            // Create the input graph
            var g = planRender.createGraph();

            function render(layout) {
              planRender.render(svgGroup, g, layout);
              svgGraph.controls.updateBBox(planRender.bbox);
            }

            // Add our custom shapes
            workflowShapes.scope = scope;

            // the hosts (graph clusters)
            var hosts = [];
            // the steps
            var steps = [];
            // data used to render errors
            var errorRenderingData = {cycles: {}, errorSteps: {}};

            function appendStepNode(g, stepName, step, hostId) {
              var shortActivityType = scope.workflows.getStepActivityType(step);
              var width, height;
              if (shortActivityType === 'CallOperationWorkflowActivity' || shortActivityType === 'DelegateWorkflowActivity' || shortActivityType === 'InlineWorkflowActivity') {
                width = 80;
                height = 60;
              } else if (shortActivityType === 'SetStateWorkflowActivity') {
                if (scope.wfViewMode === 'simple') {
                  width = 10;
                  height = 10;
                } else {
                  width = 60;
                  height = 45;
                }
              } else { // Display an alert message as this should be considered as an error.
                console.error('Unexpected activity type encountered', shortActivityType);
              }
              if (_.defined(width)) {
                g.setNode(stepName, {
                  label: '',
                  width: width,
                  height: height,
                  shape: 'operationStep',
                  parent: hostId
                });
              }
            }

            function appendEdge(g, from, to) {
              var style = {
                lineInterpolate: 'basis',
                arrowhead: 'vee',
                style: 'stroke: black; stroke-width: 1.5px;',
                pinnedStyle: 'stroke: black; stroke-width: 5px;',
                marker: 'arrow-standard'
              };
              if (errorRenderingData.cycles[from] && _.contains(errorRenderingData.cycles[from], to)) {
                // the edge is in a cycle, make it red
                style = {
                  lineInterpolate: 'basis',
                  arrowhead: 'vee',
                  style: 'stroke: #f66; stroke-width: 1.5px;',
                  pinnedStyle: 'stroke: black; stroke-width: 5px;',
                  marker: 'arrow-error'
                };
              }
              g.setEdge(from, to, style);
            }

            function refresh() {
              // remove remaining popups
              d3.selectAll('.d3-tip').remove();
              g.nodes().forEach(function (node) {
                g.removeNode(node);
              });

              if (!scope.currentWorkflowName || !scope.topology.topology.workflows || !scope.topology.topology.workflows[scope.currentWorkflowName]) {
                // TODO clear SVG
                return;
              }
              errorRenderingData = scope.workflows.getErrorRenderingData();
              workflowShapes.errorRenderingData = errorRenderingData;

              hosts = scope.topology.topology.workflows[scope.currentWorkflowName].hosts;
              steps = scope.topology.topology.workflows[scope.currentWorkflowName].steps;
              workflowShapes.steps = steps;

              var hostsMap = {};
              // add the hosts
              if (hosts) {
                for (var i = 0; i < hosts.length; i++) {
                  var host = hosts[i];
                  hostsMap[hosts[i]] = host;
                  g.setNode(host, {label: host, clusterLabelPos: 'top'});
                }
              }

              g.nodes().forEach(function (nodeKey) {
                // if the node doesn't exists anymore let's remove it
                if (_.undefined(hostsMap[nodeKey]) && _.undefined(steps[nodeKey]) &&
                  nodeKey !== 'start' && nodeKey !== 'end') {
                  g.removeNode(nodeKey);
                }
              });

              g.setNode('start', {label: '', width: 20, height: 20, shape: 'start'});
              g.setNode('end', {label: '', width: 20, height: 20, shape: 'stop'});

              var hasSteps = false;
              if (steps) {
                for (var stepName in steps) {
                  hasSteps = true;
                  var step = steps[stepName];
                  appendStepNode(g, stepName, step, step.hostId);
                  if (step.hostId) {
                    g.setParent(stepName, step.hostId);
                  }
                  if (!step.precedingSteps || step.precedingSteps.length === 0) {
                    appendEdge(g, 'start', stepName);
                  }
                  if (!step.onSuccess || step.onSuccess.length === 0) {
                    appendEdge(g, stepName, 'end');
                  } else {
                    for (var j = 0; j < step.onSuccess.length; j++) {
                      appendEdge(g, stepName, step.onSuccess[j]);
                    }
                  }
                }
              }
              if (!hasSteps) {
                appendEdge(g, 'start', 'end');
              }

              // Rendering
              render(true);

              // tooltip
              var tip = d3Tip().attr('class', 'd3-tip wf-tip').offset([-10, 0]).html(function (d) {
                return styleTooltip(d.id);
              });
              svgGroup.call(tip);
              d3.selectAll('g.node').on('mouseover', tip.show).on('mouseout', tip.hide);
            }

            // render an styled html tool tip for a given step
            var styleTooltip = function (nodeId) {
              var step = steps[nodeId];
              if (!step) {
                return nodeId;
              }
              var html = '<div>';
              html += '<h5 class="pull-left break-word">' + step.name + '</h5>';
              html += '<i class="fa pull-right">' + scope.workflows.getStepActivityTypeIcon(step) + '</i>';
              html += '<span class="clearfix"></span>';

              if (_.isEmpty(step.targetRelationship)) {
                if (!_.isEmpty(step.target)) {
                  html += '<div class="row"><div class="col-md-3">Node' + ': </div><div class="col-md-9"><b>' + step.target + '</b></div></div>';
                }
              } else {
                html += '<div class="row"><div class="col-md-3">Source' + ': </div><div class="col-md-9"><b>' + step.target + '</b></div></div>';
                html += '<div class="row"><div class="col-md-3">Relationship' + ': </div><div class="col-md-9"><b class="break-word">' + step.targetRelationship + '</b></div></div>';
                html += '<div class="row"><div class="col-md-3">Target' + ': </div><div class="col-md-9"><b>' + scope.workflows.getTargetNodeForRelationshipStep(step) + '</b></div></div>';
              }
              var stepHostId = scope.workflows.getStepHost(step);
              if (!_.isEmpty(stepHostId)) {
                html += '<div class="row"><div class="col-md-3">Host' + ': </div><div class="col-md-9"><b>' + stepHostId + '</b></div></div>';
              }
              html += '<div class="row"><div class="col-md-3">' + $translate.instant('APPLICATIONS.WF.activity') + ': </div>';
              html += '<div class="col-md-9"><b>' + $translate.instant('APPLICATIONS.WF.' + scope.workflows.getStepActivityType(step)) + '</b></div></div>';
              var activityDetails = scope.workflows.getStepActivityDetails(step);
              for (var propName in activityDetails) {
                html += '<div class="row"><div class="col-md-3">';
                html += $translate.instant('APPLICATIONS.WF.' + propName) + ': </div><div class="col-md-9 wfActivityDetail"><b>' + _.startTrunc(activityDetails[propName], 25) + '</b></div></div>';
              }
              html += '</div>';
              return html;
            };


            scope.$on('WfRefresh', function (event, args) {
              if (args.layout) {
                refresh();
              } else {
                render(false);
              }
              if (args.center) {
                centerGraph();
              }
            });

            // preview events registering
            function setPreviewEdge(g, from, to) {
              g.setEdge(from, to, {
                lineInterpolate: 'basis',
                style: 'stroke: blue; stroke-width: 3px; stroke-dasharray: 5, 5;',
                marker: 'arrow-preview'
              });
            }

            function setPreviewNode(g) {
              g.setNode('a4cPreviewNewStep', {
                style: 'stroke: blue',
                shape: 'operationPreviewStep',
                labelStyle: 'fill: blue; font-weight: bold; font-size: 2em',
                width: 60,
                height: 45
              });
            }

            scope.$on('WfRemoveEdgePreview', function (event, from, to) {
              console.debug('WfRemoveEdgePreview event received : ' + event + ', from:' + from + ', to:' + to);
              g.removeEdge(from, to);
              if (steps[from].onSuccess.length === 1) {
                setPreviewEdge(g, from, 'end');
              }
              if (steps[to].precedingSteps.length === 1) {
                setPreviewEdge(g, 'start', to);
              }
              render(true);
            });
            scope.$on('WfResetPreview', function (event) {
              console.debug('WfResetPreview event received : ' + event);
              refresh();
            });
            scope.$on('WfConnectPreview', function (event, from, to) {
              console.debug('WfConnectPreview event received : ' + event + ', from:' + from + ', to:' + to);
              for (var i = 0; i < from.length; i++) {
                g.removeEdge(from[i], 'end');
                for (var j = 0; j < to.length; j++) {
                  g.removeEdge('start', to[j]);
                  setPreviewEdge(g, from[i], to[j]);
                }
              }
              render(true);
            });
            scope.$on('WfAddStepPreview', function () {
              setPreviewNode(g);
              setPreviewEdge(g, 'start', 'a4cPreviewNewStep');
              setPreviewEdge(g, 'a4cPreviewNewStep', 'end');
              if (_.size(steps) === 0) {
                g.removeEdge('start', 'end');
              }
              render(true);
            });
            scope.$on('WfInsertStepPreview', function (event, stepId) {
              console.debug('WfInsertStepPreview event received : ' + event + ', stepId:' + stepId);
              setPreviewNode(g);
              var precedingStep;
              if (steps[stepId].precedingSteps.length === 0) {
                precedingStep = 'start';
              } else if (steps[stepId].precedingSteps.length === 1) {
                precedingStep = steps[stepId].precedingSteps[0];
              }
              if (precedingStep) {
                g.removeEdge(precedingStep, stepId);
                setPreviewEdge(g, precedingStep, 'a4cPreviewNewStep');
              }
              setPreviewEdge(g, 'a4cPreviewNewStep', stepId);
              render(true);
            });
            scope.$on('WfAppendStepPreview', function (event, stepId) {
              console.debug('WfAppendStepPreview event received : ' + event + ', stepId:' + stepId);
              setPreviewNode(g);
              var followingStep;
              if (steps[stepId].onSuccess.length === 0) {
                followingStep = 'end';
              } else if (steps[stepId].onSuccess.length === 1) {
                followingStep = steps[stepId].onSuccess[0];
              }
              if (followingStep) {
                g.removeEdge(stepId, followingStep);
                setPreviewEdge(g, 'a4cPreviewNewStep', followingStep);
              }
              setPreviewEdge(g, stepId, 'a4cPreviewNewStep');
              render(true);
            });
            scope.$on('WfRemoveStepPreview', function (event, stepId) {
              console.debug('WfRemoveStepPreview event received : ' + event + ', stepId:' + stepId);
              g.removeNode(stepId);
              var precedingSteps;
              if (!steps[stepId].precedingSteps || steps[stepId].precedingSteps.length === 0) {
                precedingSteps = ['start'];
              } else if (steps[stepId].onSuccess) {
                precedingSteps = steps[stepId].precedingSteps;
              }
              var onSuccess;
              if (!steps[stepId].onSuccess || steps[stepId].onSuccess.length === 0) {
                onSuccess = ['end'];
              } else if (steps[stepId].onSuccess) {
                onSuccess = steps[stepId].onSuccess;
              }
              for (var i = 0; i < precedingSteps.length; i++) {
                for (var j = 0; j < onSuccess.length; j++) {
                  if (precedingSteps[i] === 'start' && onSuccess[j] === 'end') {
                    continue;
                  }
                  setPreviewEdge(g, precedingSteps[i], onSuccess[j]);
                }
              }
              render(true);
            });

            function swapLinks(from, to) {
              // from's preceding become preceding of to
              var precedingSteps;
              if (!steps[from].precedingSteps || steps[from].precedingSteps.length === 0) {
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
              var onSuccess;
              if (!steps[from].onSuccess || steps[from].onSuccess.length === 0) {
                onSuccess = ['end'];
              } else {
                onSuccess = steps[from].onSuccess;
              }
              for (var j = 0; j < onSuccess.length; j++) {
                g.removeEdge(from, onSuccess[j]);
                if (onSuccess[j] !== to) {
                  setPreviewEdge(g, to, onSuccess[j]);
                }
              }
            }

            // swap steps : connections between both is inversed and each other connections are swapped
            scope.$on('WfSwapPreview', function (event, from, to) {
              console.debug('WfSwapPreview event received : ' + event + ', from:' + from + ', to:' + from);
              g.removeEdge(from, to);
              swapLinks(from, to);
              swapLinks(to, from);
              setPreviewEdge(g, to, from);
              render(true);
            });
          }
        };
      }
    ]); // factory
}); // define
