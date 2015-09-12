/** Service that provides functionalities to edit steps and edges in a workflow. */
define(function (require) {
  'use strict';
  var modules = require('modules');

  modules.get('a4c-topology-editor').factory('topoEditWf', [ 'workflowServices', '$modal', '$interval', '$filter', 'listToMapService',
    function(workflowServices, $modal, $interval, $filter, listToMapService) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
        // the current step that is displayed
        this.scope.previewWorkflowStep = undefined;
        // the current step that is pinned (on witch the user may act)
        this.scope.pinnedWorkflowStep = undefined;
//        var instance = this;
//        scope.$watch('topology', function() {
//          if (scope.currentWorkflowName) {
//            instance.cleanupSelection();
//            instance.refreshGraph();
//          }
//        }, true);
      };
      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        setCurrentWorkflowName: function(workflowName) {
          this.clearSelection();
          this.scope.currentWorkflowName = workflowName;
          // this is need in case of failure while renaming
          this.workflowName = workflowName;
          this.refreshGraph(true);
        },
        previewStep: function(step) {
          this.scope.previewWorkflowStep = step;
          this.scope.$apply();
        },
        exitPreviewStep: function() {
          if (this.scope.pinnedWorkflowStep) {
            this.scope.previewWorkflowStep = this.scope.pinnedWorkflowStep;
          } else {
            this.scope.previewWorkflowStep = undefined;
          }
          this.scope.$apply();
        },        
        // include or exclude this step from the selection
        toggleStepSelection: function(stepId) {
          var indexOfId = this.scope.workflowCurrentStepSelection.indexOf(stepId);
          var selected = false;
          if (indexOfId > -1) {
            // remove from selection
            this.scope.workflowCurrentStepSelection.splice(indexOfId, 1);
          } else {
            // add to selection
            this.scope.workflowCurrentStepSelection.push(stepId);
            selected = true;
          }
          this.scope.$apply();
          this.refreshGraph();
          return selected;
        },
        unselectStep: function(stepId) {
          var indexOfId = this.scope.workflowCurrentStepSelection.indexOf(stepId);
          if (indexOfId > -1) {
            this.scope.workflowCurrentStepSelection.splice(indexOfId, 1);
            this.refreshGraph();
          }
        },
        isStepSelected: function(stepId) {
          var indexOfId = this.scope.workflowCurrentStepSelection.indexOf(stepId);
          return (indexOfId > -1);
        },
        isStepPinned: function(stepId) {
          return (this.scope.pinnedWorkflowStep && stepId == this.scope.pinnedWorkflowStep.name);
        },
        hasStepPinned: function() {
          return this.scope.pinnedWorkflowStep;
        },        
        clearSelection: function() {
          this.scope.workflowCurrentStepSelection = [];
          this.scope.pinnedWorkflowStep = undefined;
          this.scope.previewWorkflowStep = undefined;
        },
        topologyChanged: function() {
          if (!this.scope.currentWorkflowName) {
            return;
          }
          var steps = this.scope.topology.topology.workflows[this.scope.currentWorkflowName].steps;
          if (this.scope.pinnedWorkflowStep) {
            if (!steps[this.scope.pinnedWorkflowStep.name]) {
              // this step doesn't exists no more
              this.scope.pinnedWorkflowStep = undefined;
              this.scope.previewWorkflowStep = undefined;
            }
          }
          var newSelection = [];
          for(var selectedNodeIdx in this.scope.workflowCurrentStepSelection) {
            var selectedNode = this.scope.workflowCurrentStepSelection[selectedNodeIdx];
            if (steps[selectedNode]) {
              newSelection.push(selectedNode);
            }
          }
          this.scope.workflowCurrentStepSelection = newSelection;

//          this.scope.$apply();
          this.refreshGraph();
        },
        // the current step is the one that is displayed at the right of the screen
        setPinnedWorkflowStep: function(nodeId, step) {
          this.scope.pinnedWorkflowStep = step;
          this.scope.pinnedWorkflowStep.precedingSteps = (step.precedingSteps) ? step.precedingSteps : [];
          this.scope.pinnedWorkflowStep.followingSteps = (step.followingSteps) ? step.followingSteps : [];
        },
        // return the steps that can be candidate to connect to the current pinned step
        getConnectFromCandidates: function() {
          var connectFromCandidate = [];
          var step = this.scope.pinnedWorkflowStep;
          for(var selectedNodeIdx in this.scope.workflowCurrentStepSelection) {
            var selectedNode = this.scope.workflowCurrentStepSelection[selectedNodeIdx];
            if (selectedNode !== step.name && step.precedingSteps.indexOf(selectedNode) < 0) {
              // the selected node is not in the preceding steps, we can propose it as 'from connection'
              connectFromCandidate.push(selectedNode);
            }
          }      
          return connectFromCandidate;
        },
        // return the steps that can be candidate to connect from the current pinned step
        getConnectToCandidates: function() {
          var connectToCandidate = [];
          var step = this.scope.pinnedWorkflowStep;
          for(var selectedNodeIdx in this.scope.workflowCurrentStepSelection) {
            var selectedNode = this.scope.workflowCurrentStepSelection[selectedNodeIdx];
            if (selectedNode !== step.name && step.followingSteps.indexOf(selectedNode) < 0) {
              // the selected node is not in the following steps, we can propose it as 'to connection'
              connectToCandidate.push(selectedNode);
            }
          }     
          return connectToCandidate;
        },        
        // pin or un-pin this step : when a step is pinned it remains the current step until it is un-pinned
        togglePinnedworkflowStep: function(nodeId, step) {
          if (this.scope.pinnedWorkflowStep === step) {
            this.scope.pinnedWorkflowStep = undefined;
          } else {
            this.setPinnedWorkflowStep(nodeId, step);
          }
          this.scope.$apply();
          this.refreshGraph();
        },
        unpinCurrent: function() {
          this.scope.pinnedWorkflowStep = undefined;
          this.scope.previewWorkflowStep = undefined;
          this.refreshGraph();
        },
        // === actions on workflows
        createWorkflow: function() {
          var scope = this.scope;
          var instance = this;
          workflowServices.workflows.create(
            {
              topologyId: scope.topology.topology.id
            }, {},
            function(successResult) {
              if (!successResult.error) {
                instance.clearSelection();
                scope.topology.topology.workflows[successResult.data.name] = successResult.data;
                instance.setCurrentWorkflowName(successResult.data.name);
                console.debug('operation succeded, workflow create: ' + successResult.data.name);
              } else {
                console.debug(successResult.error);
              }
            },
            function(errorResult) {
              console.debug(errorResult);
            }
          );            
        },        
        removeWorkflow: function() {
          var scope = this.scope;
          var instance = this;
          workflowServices.workflows.remove(
            {
              topologyId: scope.topology.topology.id,
              workflowName: scope.currentWorkflowName
            }, 
            function(successResult) {
              if (!successResult.error) {
                delete scope.topology.topology.workflows[scope.currentWorkflowName];
                instance.setCurrentWorkflowName(undefined);
              } else {
                console.debug(successResult.error);
              }
            },
            function(errorResult) {
              console.debug(errorResult);
            }
          );            
        },
        renameWorkflow: function(newName) {
          var scope = this.scope;
          var instance = this;
          var oldName = scope.currentWorkflowName;
          console.log("wf renaming '" + scope.currentWorkflowName + "' to '" + newName);
          workflowServices.workflows.rename(
            {
              topologyId: scope.topology.topology.id,
              workflowName: scope.currentWorkflowName,
              newName: newName
            }, {},
            function(successResult) {
              if (!successResult.error) {
                scope.topology.topology.workflows[newName] = successResult.data;
                delete scope.topology.topology.workflows[scope.currentWorkflowName];
                instance.setCurrentWorkflowName(newName);
              } else {
                instance.setCurrentWorkflowName(scope.currentWorkflowName);
                console.debug(successResult.error);
              }
            },
            function(errorResult) {
              instance.setCurrentWorkflowName(scope.currentWorkflowName);
              console.debug(errorResult);
            }
          );            
        }, 
        reinitWorkflow: function() {
          var scope = this.scope;
          var instance = this;
          workflowServices.workflows.init(
              {
                topologyId: scope.topology.topology.id,
                workflowName: scope.currentWorkflowName
              }, {},
              function(successResult) {
                if (!successResult.error) {
                  scope.topology.topology.workflows[scope.currentWorkflowName] = successResult.data;
                } else {
                  console.debug(successResult.error);
                }
              },
              function(errorResult) {
                console.debug(errorResult);
              }
            );           
        },
        // === actions on steps
        renameStep: function(stepId, newStepName) {
          var scope = this.scope;
          var instance = this;
          instance.unpinCurrent();
          workflowServices.step.rename(
            {
              topologyId: scope.topology.topology.id,
              workflowName: scope.currentWorkflowName,
              stepId: stepId,
              newStepName: newStepName
            }, {},
            function(successResult) {
              if (!successResult.error) {
                scope.topology.topology.workflows[scope.currentWorkflowName] = successResult.data;
                instance.setPinnedWorkflowStep(newStepName, scope.topology.topology.workflows[scope.currentWorkflowName].steps[newStepName]);
                console.debug('operation succeded');
              } else {
                console.debug(successResult.error);
                instance.setPinnedWorkflowStep(stepId, scope.topology.topology.workflows[scope.currentWorkflowName].steps[stepId]);
              }
            },
            function(errorResult) {
              console.debug(errorResult);
              instance.setPinnedWorkflowStep(stepId, scope.topology.topology.workflows[scope.currentWorkflowName].steps[stepId]);
            }
          );            
        },        
        removeEdge: function(from, to) {
          var scope = this.scope;
          var instance = this;
          workflowServices.edge.remove({
              topologyId: scope.topology.topology.id,
              workflowName: scope.currentWorkflowName,
              from: from,
              to: to
            },
            function(successResult) {
              if (!successResult.error) {
                var wf = successResult.data;
                if (scope.pinnedWorkflowStep) {
                  instance.setPinnedWorkflowStep(scope.pinnedWorkflowStep.name, wf.steps[scope.pinnedWorkflowStep.name]);
                }
                scope.topology.topology.workflows[scope.currentWorkflowName] = wf;
                console.debug('operation succeded');
              } else {
                console.debug(successResult.error);
              }
            },
            function(errorResult) {
              console.debug(errorResult);
            }
          );      
        },
        removeStep: function(stepId) {
          var scope = this.scope;
          var instance = this;
          workflowServices.step.remove({
              topologyId: scope.topology.topology.id,
              workflowName: scope.currentWorkflowName,
              stepId: stepId
            },
            function(successResult) {
              if (!successResult.error) {
                instance.clearSelection();
                scope.topology.topology.workflows[scope.currentWorkflowName] = successResult.data;
                console.debug('operation succeded');
              } else {
                console.debug(successResult.error);
              }
            },
            function(errorResult) {
              console.debug(errorResult);
            }
          );      
        },        
        connectFrom: function() {
          var scope = this.scope;
          var instance = this;
          var connectFromCandidate = this.getConnectFromCandidates();
          if (connectFromCandidate.length == 0) {
            return;
          }
          workflowServices.step.connectFrom({
              topologyId: scope.topology.topology.id,
              workflowName: scope.currentWorkflowName,
              stepId: scope.pinnedWorkflowStep.name
            }, angular.toJson(connectFromCandidate), 
            function(successResult) {
              if (!successResult.error) {
                var wf = successResult.data;
                if (scope.pinnedWorkflowStep) {
                  instance.setPinnedWorkflowStep(scope.pinnedWorkflowStep.name, wf.steps[scope.pinnedWorkflowStep.name]);
                }
                scope.topology.topology.workflows[scope.currentWorkflowName] = wf;
                console.debug('operation succeded');
              } else {
                console.debug(successResult.error);
              }
            },
            function(errorResult) {
              console.debug(errorResult);
            }
          );       
        },
        connectTo: function() {
          var scope = this.scope;
          var instance = this;
          var connectToCandidate = this.getConnectToCandidates();
          if (connectToCandidate.length == 0) {
            return;
          }          
          workflowServices.step.connectTo({
              topologyId: scope.topology.topology.id,
              workflowName: scope.currentWorkflowName,
              stepId: scope.pinnedWorkflowStep.name
            }, angular.toJson(connectToCandidate),
            function(successResult) {
              if (!successResult.error) {
                var wf = successResult.data;
                if (scope.pinnedWorkflowStep) {
                  instance.setPinnedWorkflowStep(scope.pinnedWorkflowStep.name, wf.steps[scope.pinnedWorkflowStep.name]);
                }
                scope.topology.topology.workflows[scope.currentWorkflowName] = wf;
                console.debug('operation succeded');
              } else {
                console.debug(successResult.error);
              }
            },
            function(errorResult) {
              console.debug(errorResult);
            }
          );       
        },
        swap: function(from, to) {
          var scope = this.scope;
          var instance = this;          
          workflowServices.step.swap({
            topologyId: scope.topology.topology.id,
            workflowName: scope.currentWorkflowName,
            stepId: from,
            targetId: to
          }, {},
          function(successResult) {
            if (!successResult.error) {
              var wf = successResult.data;
              scope.topology.topology.workflows[scope.currentWorkflowName] = wf;
              if (scope.pinnedWorkflowStep) {
                instance.setPinnedWorkflowStep(scope.pinnedWorkflowStep.name, wf.steps[scope.pinnedWorkflowStep.name]);
              }              
              console.debug('operation succeded');
            } else {
              console.debug(successResult.error);
            }
          },
          function(errorResult) {
            console.debug(errorResult);
          }
        );           
          
        },
        addOperation: function() {
          this.addOperationActivity();         
        },        
        appendOperation: function(stepId) {
          this.addOperationActivity(stepId, false);
        },
        insertOperation: function(stepId) {
          this.addOperationActivity(stepId, true);
        },        
        addOperationActivity: function(stepId, before) {
          var scope = this.scope;
          var instance = this;
          var modalInstance = $modal.open({
            templateUrl: 'views/topology/workflow_operation_selector.html',
            controller: 'WfOperationSelectorController',
            resolve: {
              topologyDTO: function() {
                return scope.topology;
              }
            }
          });
          modalInstance.result.then(function(trilogy) {
            var activityRequest = {
                relatedStepId: stepId,
                before: before,
                activity: {
                  type: 'alien4cloud.paas.wf.OperationCallActivity',
                  nodeId: trilogy.node,
                  interfaceName: trilogy.interface,
                  operationName: trilogy.operation
                }
            };
            instance.addActivity(activityRequest);
          });           
        },
        addActivity: function(activityRequest) {
          var scope = this.scope;
          var instance = this;
          workflowServices.activity.add({
              topologyId: scope.topology.topology.id,
              workflowName: scope.currentWorkflowName
            }, activityRequest,
            function(successResult) {
              if (!successResult.error) {
                var wf = successResult.data;
                if (scope.pinnedWorkflowStep) {
                  instance.setPinnedWorkflowStep(scope.pinnedWorkflowStep.name, wf.steps[scope.pinnedWorkflowStep.name]);
                }
                scope.topology.topology.workflows[scope.currentWorkflowName] = wf;
                console.debug('operation succeded');
              } else {
                console.debug(successResult.error);
              }
            },
            function(errorResult) {
              console.debug(errorResult);
            }
          );       
        },        
        appendState: function(stepId) {
          this.addStateActivity(stepId, false);
        },
        insertState: function(stepId) {
          this.addStateActivity(stepId, true);
        }, 
        addState: function() {
          this.addStateActivity();
        },          
        addStateActivity: function(stepId, before) {
          var scope = this.scope;
          var instance = this;
          var modalInstance = $modal.open({
            templateUrl: 'views/topology/workflow_state_selector.html',
            controller: 'WfStateSelectorController',
            resolve: {
              topologyDTO: function() {
                return scope.topology;
              }
            }
          });
          modalInstance.result.then(function(trilogy) {
            var activityRequest = {
                relatedStepId: stepId,
                before: before,
                activity: {
                  type: 'alien4cloud.paas.wf.SetStateActivity',
                  nodeId: trilogy.node,
                  stateName: trilogy.state
                }
            };
            instance.addActivity(activityRequest);
          }); 
        },
        // refresh graph
        refreshGraph: function(center) {
          this.scope.$broadcast('WfRefresh');
          if (center) {
            this.scope.$broadcast('WfCenterGraph');
          }
        },  
        // === action preview
        removeStepPreview: function(stepId) {
          this.scope.$broadcast('WfRemoveStepPreview', stepId);
        },        
        removeEdgePreview: function(from, to) {
          this.scope.$broadcast('WfRemoveEdgePreview', from, to);
        },
        connectFromPreview: function() {
          var candidates = this.getConnectFromCandidates();
          if (candidates && candidates.length > 0) {
            this.connectPreview(candidates, [this.scope.pinnedWorkflowStep.name]);
          }
        },
        connectToPreview: function() {
          var candidates = this.getConnectToCandidates();
          if (candidates && candidates.length > 0) {
            this.connectPreview([this.scope.pinnedWorkflowStep.name], candidates);
          }
        },
        swapPreview: function(from, to) {
          this.scope.$broadcast('WfSwapPreview', from, to);
        },
        connectPreview: function(from, to) {
          this.scope.$broadcast('WfConnectPreview', from, to);
        }, 
        addStepPreview: function() {
          this.scope.$broadcast('WfAddStepPreview');
        }, 
        insertStepPreview: function(stepId) {
          this.scope.$broadcast('WfInsertStepPreview', stepId);
        }, 
        appendStepPreview: function(stepId) {
          this.scope.$broadcast('WfAppendStepPreview', stepId);
        },        
        resetPreview: function() {
          this.scope.$broadcast('WfResetPreview');
        },        
        
        // ===== misc : should probably be removed from here
        getStepNodeIcon: function(step) {
          var nodeName = step.nodeId;
          if (this.scope.topology.topology.nodeTemplates[nodeName]) {
            var typeName = this.scope.topology.topology.nodeTemplates[nodeName].type;
            var nodeType = this.scope.topology.nodeTypes[typeName];
            var tags = listToMapService.listToMap(nodeType.tags, 'name', 'value');
            return tags.icon;
          } else {
            return undefined;
          }
        },
        getStepActivityTypeIcon: function(step) {
          var shortType = this.getStepActivityType(step);
          if (shortType === 'OperationCallActivity') {
            return '\uf085'; // fa-cogs
          } else if (shortType === 'SetStateActivity') {
            return '\uf087'; // fa-thumbs-o-up
          } else {
            return '\uf1e2'; // fa-bomb
          }            
        },        
        getStepActivityType: function(step) {
          return $filter('splitAndGet')(step.activity.type, '.', 'last');
        },
        getStepActivityDetails: function(step) {
          var details = {};
          var shortType = this.getStepActivityType(step);
          if (shortType === 'OperationCallActivity') {
            details['interfaceName'] = step.activity.interfaceName;
            details['operationName'] = step.activity.operationName;
          } else if (shortType === 'SetStateActivity') {
            details['stateName'] = step.activity.stateName;
          } else {
            details = step.activity;
          }     
          return details;
        },        
        getErrorType: function(error) {
          return $filter('splitAndGet')(error.type, '.', 'last');
        },
        // build a data structure to ease errors rendering inside d3 graph
        getErrorRenderingData: function() {
          // cycles: a map 'from' -> 'to' array of edges in cycle
          // errorSteps: map where keys are steps that are known as bad sequenced state steps
          var result = {cycles: {}, errorSteps: {}};
          var errors = this.scope.topology.topology.workflows[this.scope.currentWorkflowName].errors;
          var instance = this;
          if (errors) {
            errors.forEach(function(error) {
              if (instance.getErrorType(error) === 'WorkflowHasCycleError') {
                var lastStep = undefined;
                for(var stepCycleIdx in error.cycle) {
                  var stepName = error.cycle[stepCycleIdx];
                  if (!lastStep) {
                    lastStep = stepName;
                  } else {
                    if (result.cycles[lastStep]) {
                      result.cycles[lastStep].push(stepName);
                    } else {
                      result.cycles[lastStep] = [stepName];
                    }
                    lastStep = stepName;
                  }
                }
              } else if (instance.getErrorType(error) === 'BadStateSequenceError') {
                result.errorSteps[error.from] = true;
                result.errorSteps[error.to] = true;
              } else if (instance.getErrorType(error) === 'UnknownNodeError') {
                result.errorSteps[error.stepId] = true;
              }
            });
          }
          return result;
        }
      }

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.workflows = instance;
      };
    }
  ]); // modules
}); // define
