define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var d3 = require('d3');

  require('scripts/common/services/list_to_map_service');

  modules.get('a4c-topology-editor').factory('workflowShapes', ['runtimeColorsService', 'toscaService', 'd3Service',
    function(runtimeColorsService, toscaService, d3Service) {
      // Defines shapes to be rendered for workflow nodes.
      return {
        steps: [],
        errorRenderingData: {cycles: {}, errorSteps: {}},
        start: function(parent) {
          parent.insert('circle', ':first-child').attr('cx', '0').attr('cy', '0').attr('r', 10).attr('style', 'fill:none; stroke:green; stroke-width:3');
        },
        stop: function(parent) {
          var r = 10;
          var r2 = r * 0.6;
          parent.insert('circle').attr('cx', '0').attr('cy', '0').attr('r', r).attr('style', 'fill:none; stroke:'+runtimeColorsService.started+'; stroke-width:2');
          parent.append('circle').attr('cx', '0').attr('cy', '0').attr('r', r2).attr('style', 'fill:'+runtimeColorsService.started+'; stroke:'+runtimeColorsService.started+'; stroke-width:1');
        },
        operationPreviewStep: function(parent, node) {
          var x = (node.width / 2) * -1;
          var w = node.width;
          var y = (node.height / 2) * -1;
          var h = node.height;
          var shapeSvg = d3Service.rect(parent, x, y, w, h, 5, 5);
          shapeSvg.style('stroke', 'blue');
          parent.append('text').attr('class', 'wfOperationLabel').text('?').style('text-anchor', 'middle');
        },
        operationStep: function(parent, node) {
          var steps = this.steps, scope = this.scope, errorRenderingData = this.errorRenderingData;
          var nodeId = node.id;
          var step = steps[nodeId];
          var nodeName = step.target;
          var nodeType;
          if (scope.topology.topology.nodeTemplates[nodeName]) {
            var typeName = scope.topology.topology.nodeTemplates[nodeName].type;
            nodeType = scope.topology.nodeTypes[typeName];
          }

          var shortActivityType = scope.workflows.getStepActivityType(step);
          var simpleView = (scope.wfViewMode === 'simple' && shortActivityType === 'SetStateWorkflowActivity');

          var x = (node.width / 2) * -1;
          var w = node.width;
          var y = (node.height / 2) * -1;
          var h = node.height;
          if (simpleView) {
            x = 0;
            y = 0;
            w = 2;
            h = 2;
          }

          // TODO better use CSS rather than fill and stroke
          var shapeSvg = undefined;
          if (simpleView) {
            shapeSvg = d3Service.circle(parent, x, y, 2).attr('style', 'fill:DarkGray; stroke:DarkGray;');
          } else {
            shapeSvg = d3Service.rect(parent, x, y, w, h, 2, 2);
            //shapeSvg = d3Service.circle(parent, x, y, 10).attr('class', 'connectorAction');
          }

          // var shapeSvg = parent.insert('rect').attr('x', x).attr('y', y).attr('width', w).attr('height', h).attr('rx', 5).attr('ry', 5).style('fill', 'white');
          if (errorRenderingData.errorSteps[nodeId]) {
            // the step is in a bad sequence, make it red
            shapeSvg.style('stroke', '#f66');
          } else {
            if (scope.wfViewMode === 'simple'){
              shapeSvg.style('stroke', 'DarkGray');
            } else {
              shapeSvg.style('stroke', 'grey');
            }
          }
          var iconSize = 25;
          var icon = undefined;
          if (simpleView){
            //icon = parent.append('text').attr('class', 'fa').attr('x', x + 8).attr('y', y + 17).text(scope.workflows.getStepActivityTypeIcon(step));
          } else {
            icon = parent.append('text').attr('class', 'fa').attr('x', x + w - 22).attr('y', y + 16).text(scope.workflows.getStepActivityTypeIcon(step));
          }
          if (shortActivityType === 'CallOperationWorkflowActivity') {
            parent.append('text').attr('class', 'wfOperationLabel').attr('y', y + h - 10).text(_.trunc(step.activities[0].operationName, {'length': 10})).style('text-anchor', 'middle');
          } else if (shortActivityType === 'DelegateWorkflowActivity') {
            parent.append('text').attr('class', 'wfDelegateLabel').attr('fill', '#7A7A52').attr('y', y + h - 10).text(_.trunc(step.activities[0].workflowName, {'length': 10})).style('text-anchor', 'middle');
          } else if (shortActivityType === 'SetStateWorkflowActivity' && !simpleView) {
            parent.append('text').attr('class', 'wfStateLabel').attr('fill', '#003399').attr('y', y + h - 8).text(_.trunc(step.activities[0].stateName, {'length': 13})).style('text-anchor', 'middle');
            iconSize = 16;
          } else if (!simpleView) {
            parent.append('text').attr('class', 'wfDelegateLabel').attr('fill', '#7A7A52').attr('y', y + h - 10).text(_.trunc(step.activities[0].inline, {'length': 10})).style('text-anchor', 'middle');
          }
          if (nodeType && nodeType.tags && !simpleView) {
            var nodeIcon = toscaService.getIcon(nodeType.tags);
            if (_.defined(nodeIcon)) {
              parent.append('image').attr('x', x + 5).attr('y', y + 5).attr('width', iconSize).attr('height', iconSize).attr('xlink:href',
                'img?id=' + nodeIcon + '&quality=QUALITY_64');
            }
          }

          var onMouseOver = function() {
            scope.workflows.previewStep(steps[nodeId]);
          };
          var onMouseOut = function() {
            scope.workflows.exitPreviewStep();
          };
          var onClick = function() {
            var stepPinned = scope.workflows.isStepPinned(nodeId);
            var stepSelected = scope.workflows.isStepSelected(nodeId);
            var hasStepPinned = scope.workflows.hasStepPinned();
            if (scope.workflows.isRuntimeMode()) {
                scope.workflows.togglePinnedworkflowStep(nodeId, steps[nodeId]);
                //scope.workflows.setPinnedWorkflowStep(nodeId, steps[nodeId]);
            } else {
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
            }
          };

          // add a transparent rect for event handling
          d3Service.rect(parent, x, y, w, h, 5, 5).attr('class', 'selector').on('click', onClick).on('mouseover', onMouseOver).on('mouseout', onMouseOut);
          return shapeSvg;
        },
        operationStepUpdate: function(parent, node) {
            // TODO: here check the mode (editor or runtime, if runtime, check step status
          var backgroundRect = parent.select('rect');

          backgroundRect.style('fill', 'white');
          if (this.scope.workflows.isEditorMode()) {
              if (this.scope.workflows.isStepPinned(node.id)) {
                backgroundRect.style('fill', '#CCE0FF');
              } else if (this.scope.workflows.isStepSelected(node.id)) {
                backgroundRect.style('fill', '#FFFFD6');
              }
          } else if (this.scope.workflows.isRuntimeMode()) {
            var stepStatus = this.getStepStatus(node.id);
            if (_.defined(stepStatus)) {
                parent.selectAll('text').style("fill", "white");

                switch (String(stepStatus)) {
                    case "STARTED":
                        backgroundRect.style('fill', '#428bca'); // blue Cf. alert-info border
                        break;
                    case "COMPLETED_SUCCESSFULL":
                        backgroundRect.style('fill', '#5cb85c'); // green Cf. alert-success border  #d6e9c6
                        break;
                    case "COMPLETED_WITH_ERROR":
                        backgroundRect.style('fill', '#c9302c'); // red Cf. alert-danger border
                        break;
                }
            }
          }
        },
        edge: function(parent) {
          var self = this;
          parent.append('path')
            .attr('class', 'path')
            .attr('d', function(e) {
              var points = e.points;
              return self.createLine(e, points);
            });
          parent.on('click', function (edge) {
            if (edge.source.id !== 'start' && edge.target.id !== 'end') {
              // edge connected to start or end are no editable
              self.scope.workflows.togglePinEdge(edge.source.id, edge.target.id);
            }
          });
        },
        edgeUpdate: function(parent) {
          var self = this;
          var path = parent.select('path');
          path.attr('d', function(e) {
            var points = e.points;
            return self.createLine(e, points);
          });
          path.attr('style', function(e) {
            if (e.pinnedStyle && self.scope.wfPinnedEdge) {
              if (self.scope.wfPinnedEdge.from === e.source.id && self.scope.wfPinnedEdge.to === e.target.id) {
                return e.pinnedStyle;
              }
            }
            return e.style;
          });
          path.attr('marker-end', function(e) {
            if (e.pinnedStyle && self.scope.wfPinnedEdge) {
              if (self.scope.wfPinnedEdge.from === e.source.id && self.scope.wfPinnedEdge.to === e.target.id) {
                return 'url(#'+e.marker+'-pinned)';
              }
            }
            return 'url(#'+e.marker+')';
          });
        },

        createLine: function(edge, points) {
          var line = d3.svg.line()
            .x(function(d) { return d.x; })
            .y(function(d) { return d.y; });

          if (_.has(edge, 'lineInterpolate')) {
            line.interpolate(edge.lineInterpolate);
          }

          if (_.has(edge, 'lineTension')) {
            line.tension(Number(edge.lineTension));
          }

          return line(points);
        },

        initMarkers: function(svg) {
          var defs = svg.append('defs');
          function addMarker(name, color, large) {
            var marker = defs.append('marker')
              .attr('id', name)
              .attr('markerUnits', 'userSpaceOnUse').attr('orient', 'auto');
            var path = marker.append('path').attr('style', function() {return 'fill: '+color+'; stroke: '+color; });
            if(large) {
              marker.attr('markerWidth', '10')
                .attr('markerHeight', '20')
                .attr('refX', '10')
                .attr('refY', '10');
              path.attr('d', 'M0,0 L0,20 L12,10 z');
            } else {
              marker.attr('markerWidth', '5')
                .attr('markerHeight', '10')
                .attr('refX', '5')
                .attr('refY', '5');
              path.attr('d', 'M0,0 L0,10 L6,5 z');
            }
          }
          addMarker('arrow-standard', 'black', false);
          addMarker('arrow-standard-pinned', 'black', true);
          addMarker('arrow-error', '#f66', false);
          addMarker('arrow-error-pinned', '#f66', true);
          addMarker('arrow-preview', 'blue', false);
        },

        getStepStatus: function(nodeId) {
          var self = this;
          var stepStatus = undefined;
          if (self.scope.workflows.isRuntimeMode()) {
            var monitoringData = self.scope.workflows.getMonitoringData();
            if (_.defined(monitoringData) && monitoringData.stepStatus.hasOwnProperty(nodeId)) {
                stepStatus = monitoringData.stepStatus[nodeId];
            }
          }
          return stepStatus;
        }
      };
    } // function
  ]); // factory
}); // define
