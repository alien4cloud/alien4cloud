// Directive allowing to display a workflow (tosca plan)
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('d3');
  require('js-lib/dagre-d3');

  require('scripts/common-graph/services/runtime_color_service');
  require('scripts/common-graph/services/svg_service');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.bootstrap', 'a4c-styles']).directive(
    'topologyPlan', ['$filter', '$http', '$modal', 'svgServiceFactory', 'runtimeColorsService', 'bboxFactory',
    function($filter, $http, $modal, svgServiceFactory, runtimeColorsService, bboxFactory) {
      return {
        restrict : 'E',
        scope : {
          topology: '=',
          dimensions: '='
        },
        link : function(scope) {
          // Default parent svg markup to render the topology
          var containerElement = d3.select('#plan-graph-container');

          var svgGraph = svgServiceFactory.create(containerElement, 'plan-svg', 'plan-svg');

          var defs = svgGraph.svg.append('defs');
          defs.append('g').attr('id', 'start-shape')
          .append('circle').attr('cx', '0').attr('cy', '0').attr('r', '12').attr('style', 'fill:none; stroke:green; stroke-width:3');
          var definition = defs.append('g').attr('id', 'stop-shape');
          definition.append('circle').attr('cx', '0').attr('cy', '0').attr('r', '12').attr('style', 'fill:none; stroke:'+runtimeColorsService.started+'; stroke-width:2');
          definition.append('circle').attr('cx', '0').attr('cy', '0').attr('r', '9').attr('style', 'fill:'+runtimeColorsService.started+'; stroke:'+runtimeColorsService.started+'; stroke-width:1');

          definition =  defs.append('g').attr('id', 'parallel-gateway-shape');
          definition.append('rect').attr('x', '-12').attr('y', '-12').attr('width', '24').attr('height', '24').attr('style', 'fill:none; stroke:black; stroke-width:1').attr('transform', 'rotate(45, 0, 0)');
          definition.append('path').attr('d', 'M-10 0 L10 0 Z').attr('style', 'fill:none; stroke:black; stroke-width:2');
          definition.append('path').attr('d', 'M0 -10 L0 10 Z').attr('style', 'fill:none; stroke:black; stroke-width:2');

          definition =  defs.append('g').attr('id', 'parallel-join-gateway-shape');
          definition.append('rect').attr('x', '-12').attr('y', '-12').attr('width', '24').attr('height', '24').attr('style', 'fill:none; stroke:blue; stroke-width:1').attr('transform', 'rotate(45, 0, 0)');
          definition.append('path').attr('d', 'M-10 0 L10 0 Z').attr('style', 'fill:none; stroke:blue; stroke-width:2');
          definition.append('path').attr('d', 'M0 -10 L0 10 Z').attr('style', 'fill:none; stroke:blue; stroke-width:2');

          definition =  defs.append('g').attr('id', 'event-based-gateway-shape');
          definition.append('rect').attr('x', '-12').attr('y', '-12').attr('width', '24').attr('height', '24').attr('style', 'fill:none; stroke: black; stroke-width:1').attr('transform', 'rotate(45, 0, 0)');
          definition.append('circle').attr('cx', '0').attr('cy', '0').attr('r', '10').attr('style', 'fill:none; stroke:black; stroke-width:1');
          definition.append('circle').attr('cx', '0').attr('cy', '0').attr('r', '8').attr('style', 'fill:none; stroke:black; stroke-width:1');

          for(var state in runtimeColorsService) {
            if(runtimeColorsService.hasOwnProperty(state)) {
              var color = runtimeColorsService[state];
              definition = defs.append('g').attr('id', 'state-update-shape-'+state);
              definition.append('circle').attr('cx', '0').attr('cy', '0').attr('r', '12').attr('style', 'fill:none; stroke:'+color+'; stroke-width:2');
              definition.append('circle').attr('cx', '0').attr('cy', '0').attr('r', '9').attr('style', 'fill:none; stroke:'+color+'; stroke-width:2');
              definition.append('rect').attr('x', '-5').attr('y', '-4').attr('width', '10').attr('height', '8').attr('style', 'fill:none; stroke:'+color+'; stroke-width:1');
              definition.append('path').attr('d', 'M-5 -4 L0 0 L5 -4 Z').attr('style', 'fill:none; stroke:'+color+'; stroke-width:1');
            }
          }

          var eventProcessors = {};

          function simpleName(typeName) {
            return typeName.substring(typeName.lastIndexOf('.') + 1);
          }

          function processStep(graph, currentStep, previousStep) {
            var type = currentStep.type ? simpleName(currentStep.type) : 'StartEvent';
            console.log(type);
            return eventProcessors[type](graph, currentStep, previousStep);
          }

          var idGenerator = 0;

          function processNextStep(graph, currentStep, previousOverride) {
            if(_.undefined(previousOverride)) {
              previousOverride = currentStep;
            }
            if(_.defined(currentStep.nextStep)) {
              return processStep(graph, currentStep.nextStep, previousOverride);
            }
            return previousOverride;
          }

          eventProcessors.StartEvent = function (graph, startEvent) {
            startEvent.id = 'start';
            graph.nodes.push({id: startEvent.id, def: {label: 'start', useDef: 'start-shape'}});
            if(_.defined(startEvent.nextStep)) {
              return processStep(graph, startEvent.nextStep, startEvent);
            }
            return startEvent;
          };

          eventProcessors.ParallelGateway = function(graph, gateway, previousStep) {
            if(gateway.parallelSteps.length === 0) {
              return processNextStep(graph, gateway, previousStep);
            }
            var parallel;
            if(gateway.parallelSteps.length === 1) {
              parallel = processStep(graph, gateway.parallelSteps[0], previousStep);
              return processNextStep(graph, gateway, parallel);
            }
            // var parallel;
            idGenerator++;
            var joinStep = {id: 'pj_'+idGenerator};
            gateway.id = 'pg_'+idGenerator;
            graph.nodes.push({id: gateway.id, def: {label: 'pg', useDef: 'parallel-gateway-shape', clickable: false}});
            // graph.nodes.push({id: joinStep.id, def: {label: 'pj', useDef: 'parallel-gateway-shape', clickable: false}});
            graph.edges.push({from: previousStep.id, to: gateway.id, def: {label: ''}});
            for(var i=0; i<gateway.parallelSteps.length; i++) {
              parallel = processStep(graph, gateway.parallelSteps[i], gateway);
              // join from last chained elements.
              // graph.edges.push({from: parallel.id, to: joinStep.id, def: {label: ''}});
            }
            return processNextStep(graph, gateway, joinStep);
          };

          eventProcessors.StopEvent = function(graph, stopEvent, previousStep) {
            stopEvent.id = 'stop';
            graph.nodes.push({id: stopEvent.id, def: {label: 'stop', useDef: 'stop-shape', clickable: false}});
            graph.edges.push({from: previousStep.id, to: stopEvent.id, def: {label: ''}});
            return stopEvent;
          };

          eventProcessors.StateUpdateEvent = function(graph, stateUpdateEvent, previousStep) {
            stateUpdateEvent.id = stateUpdateEvent.elementId+'::'+stateUpdateEvent.state;

            var htmlLabel = '<div class="plan-box plan-state" style="border-color: '+runtimeColorsService[stateUpdateEvent.state]+'">';
            htmlLabel += '<div id="plan-state-'+stateUpdateEvent.id+'">'+stateUpdateEvent.elementId+'</div>';
            htmlLabel += '<div>'+stateUpdateEvent.state+'</div>';
            htmlLabel += '</div>';
            graph.nodes.push({id: stateUpdateEvent.id, def: { label: htmlLabel, clickable: false}});

            graph.edges.push({from: previousStep.id, to: stateUpdateEvent.id, def: {label: ''}});
            return processNextStep(graph, stateUpdateEvent);
          };

          eventProcessors.OperationCallActivity = function(graph, opCallActivity, previousStep) {
            // we want to display the node deven if no implem artifact
            if(true || _.defined(opCallActivity.implementationArtifact)) {
              idGenerator++;
              opCallActivity.id = 'oca_'+idGenerator;
              var htmlLabel = '<div class="plan-box plan-operation">';
              htmlLabel += '<div><span class="plan-icon">\uf1b2</span><span class="text-info"> '+opCallActivity.nodeTemplateId+'</span></div>';
              if(_.defined(opCallActivity.relationshipId)) {
                htmlLabel += '<div><span class="plan-icon">\uf0c1</span><span class="text-info"> '+opCallActivity.relationshipId+'</span></div>';
              }
              htmlLabel += '<div><span class="plan-icon">\uf085</span><span class="text-info"> '+$filter('splitAndGet')(opCallActivity.interfaceName, '.', 'last')+'</span></div>';
              htmlLabel += '<div><span class="plan-icon">\uf013</span><span class="text-info"> '+opCallActivity.operationName+'</span></div>';
              htmlLabel += '</div>';
              graph.nodes.push(
              {
                id: opCallActivity.id,
                def: {
                  label: htmlLabel,
                  nodeTemplateId: opCallActivity.elementId,
                  clickable: true,
                  fullpath: (opCallActivity.implementationArtifact) ? opCallActivity.implementationArtifact.artifactRef : "",
                  archiveName: (opCallActivity.implementationArtifact) ? opCallActivity.implementationArtifact.archiveName : "",
                  archiveVersion: (opCallActivity.implementationArtifact) ? opCallActivity.implementationArtifact.archiveVersion: ""
                }
              });
              graph.edges.push({from: previousStep.id, to: opCallActivity.id, def: {label: ''}});
              return processNextStep(graph, opCallActivity);
            }
            return processNextStep(graph, opCallActivity, previousStep);
          };

          eventProcessors.RelationshipTriggerEvent = function(graph, opCallActivity, previousStep) {
            if(_.defined(opCallActivity.sideOperationImplementationArtifact)) {
              idGenerator++;
              opCallActivity.id = 'oca_'+idGenerator;
              var htmlLabel = '<div class="plan-box plan-operation">';
              htmlLabel += '<div><span class="plan-icon">\uf1b2</span><span class="text-info"> '+opCallActivity.nodeTemplateId+'</span></div>';
              if(_.defined(opCallActivity.relationshipId)) {
                htmlLabel += '<div><span class="plan-icon">\uf0c1</span><span class="text-info"> '+opCallActivity.relationshipId+'</span></div>';
              }
              htmlLabel += '<div><span class="plan-icon">\uf085</span><span class="text-info"> '+$filter('splitAndGet')(opCallActivity.interfaceName, '.', 'last')+'</span></div>';
              htmlLabel += '<div><span class="plan-icon">\uf013</span><span class="text-info"> '+opCallActivity.sideOperationName+'</span></div>';
              htmlLabel += '</div>';
              graph.nodes.push(
              {
                id: opCallActivity.id,
                def: {
                  label: htmlLabel,
                  nodeTemplateId: opCallActivity.nodeTemplateId,
                  clickable: true,
                  fullpath: opCallActivity.sideOperationImplementationArtifact.artifactRef,
                  archiveName: opCallActivity.sideOperationImplementationArtifact.archiveName,
                  archiveVersion: opCallActivity.sideOperationImplementationArtifact.archiveVersion
                }
              });
              graph.edges.push({from: previousStep.id, to: opCallActivity.id, def: {label: ''}});
              return processNextStep(graph, opCallActivity);
            }
            return processNextStep(graph, opCallActivity, previousStep);
          };

          eventProcessors.ParallelJoinStateGateway = function(graph, pjsGateway, previousStep) {
            idGenerator++;
            pjsGateway.id = 'pjsg_'+idGenerator;
            graph.nodes.push({id: pjsGateway.id, def: {label: 'pjsg', useDef: 'parallel-join-gateway-shape', clickable: false}});
            graph.edges.push({from: previousStep.id, to: pjsGateway.id, def: {label: ''}});
            for(var nodeTemplateId in pjsGateway.validStatesPerElementMap) {
              if(pjsGateway.validStatesPerElementMap.hasOwnProperty(nodeTemplateId)) {
                var states = pjsGateway.validStatesPerElementMap[nodeTemplateId];
                var state = states[0];
                graph.edges.push({from: nodeTemplateId+'::'+state, to: pjsGateway.id, def: {label: ''}});
              }
            }

            return processNextStep(graph, pjsGateway);
          };

          function openArchiveModal(archiveName, archiveVersion, scriptReference) {
            var openOnFile = scriptReference ? scriptReference : null;
            $modal.open({
              templateUrl: 'views/components/csar_explorer.html',
              controller: 'CsarExplorerController',
              windowClass: 'searchModal',
              resolve: {
                archiveName: function() {
                  return archiveName;
                },
                archiveVersion: function() {
                  return archiveVersion;
                },
                openOnFile: function() {
                  return openOnFile;
                }
              }
            });
          }

          function refresh() {
            $http.get('rest/topologies/' + scope.topology.topology.id + '/startplan').success(function(planResult) {
                var currentStep = planResult.data;
                var previousStep = null;

                var graphData = { nodes: [], edges: [] };
                processStep(graphData, currentStep, previousStep);

                var graph = new dagreD3.Digraph();
                var i;
                for(i=0;i<graphData.nodes.length;i++) {
                  var node = graphData.nodes[i];
                  graph.addNode(node.id, node.def);
                }

                for(i=0;i<graphData.edges.length;i++) {
                  var edge = graphData.edges[i];
                  graph.addEdge(null, edge.from, edge.to, edge.def);
                }

                var svg = d3.select('svg'),
                svgGroup = svg.append('g');

                var layout = dagreD3.layout().nodeSep(20).rankDir('LR');
                var renderer = new dagreD3.Renderer();
                var oldDrawNode = renderer.drawNodes();
                renderer.drawNodes(function(graph, svg) {
                  var svgNodes = oldDrawNode(graph, svg);

                  svgNodes.selectAll('rect').remove();

                  svgNodes.on('click', function(d){
                    var node = graph.node(d);
                    if(node.clickable) {
                      openArchiveModal(node.archiveName, node.archiveVersion, node.fullpath);
                    }
                  });

                  return svgNodes;
                });

                renderer.zoomSetup(function(graph, svg){ return svg; });
                var rendered = renderer.layout(layout).run(graph, svgGroup);

                var borderEdgeSize = 2;
                var bbox = bboxFactory.create(-1*borderEdgeSize , borderEdgeSize, rendered.graph().width, rendered.graph().height);
                svgGraph.controls.coordinateUtils.bbox = bbox;
                svgGraph.controls.coordinateUtils.reset();
                svgGraph.controls.updateViewBox();
              });
          }

          scope.$watch('topology', function() {
            refresh();
          }, true);

          function onResize(width, height) {
            svgGraph.onResize(width, height);
          }

          scope.$watch('dimensions', function(dimensions) {
            onResize(dimensions.width, dimensions.height);
          });
          svgGraph.onResize(scope.dimensions.width, scope.dimensions.height);
        }
      };
    }
  ]); // factory
}); // define
