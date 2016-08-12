/** Service that provides functionalities to edit steps and edges in a workflow. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var angular = require('angular');

  modules.get('a4c-topology-editor').factory('topoEditWf', [ '$modal', '$interval', '$filter', 'listToMapService',
    function($modal, $interval, $filter, listToMapService) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
        // the current step that is displayed
        this.scope.previewWorkflowStep = undefined;
        // the current step that is pinned (on witch the user may act)
        this.scope.pinnedWorkflowStep = undefined;

        this.scope.wfViewMode = 'full';
        this.scope.wfPinnedEdge = undefined;

        // fa-battery-0 f244
        // fa-battery-1 f243
        // fa-battery-2 f242
        // fa-battery-3 f241
        // fa-battery-4 f240
        this.stateIcon = {
          initial: '\uf244',
          creating: '\uf243',
          created: '\uf242',
          configuring: '\uf242',
          configured: '\uf241',
          starting: '\uf241',
          started: '\uf240',
          stopping: '\uf241',
          stopped: '\uf242',
          deleting: '\uf243',
          deleted: '\uf244'
        };
      };
      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        setCurrentWorkflowName: function(workflowName) {
          this.clearSelection();
          // the given name is undefined, let select the first wf in the list
          if (_.undefined(workflowName)) {
            var wfNames = Object.keys(this.scope.topology.topology.workflows);
            if (wfNames.length > 0) {
              workflowName = wfNames[0];
            }
          }
          this.scope.currentWorkflowName = workflowName;
          // this is need in case of failure while renaming
          this.workflowName = workflowName;
          this.refreshGraph(true, true);
        },
        switchViewMode: function() {
          if (this.scope.wfViewMode === 'simple') {
            this.scope.wfViewMode = 'full';
          } else {
            this.scope.wfViewMode = 'simple';
          }
          this.refreshGraph(true, true);
        },
        previewStep: function(step) {
          this.scope.previewWorkflowStep = step;
          this.scope.$digest();
        },
        exitPreviewStep: function() {
          if (this.scope.pinnedWorkflowStep) {
            this.scope.previewWorkflowStep = this.scope.pinnedWorkflowStep;
          } else {
            this.scope.previewWorkflowStep = undefined;
          }
          this.scope.$digest();
        },
        togglePinEdge: function(from, to) {
          if (this.scope.wfPinnedEdge) {
            if (this.scope.wfPinnedEdge.from === from && this.scope.wfPinnedEdge.to === to) {
              this.scope.wfPinnedEdge = undefined;
            } else {
              this.scope.wfPinnedEdge = {'from': from, 'to': to};
            }
          } else {
            this.scope.wfPinnedEdge = {'from': from, 'to': to};
          }
          this.scope.$digest();
          this.refreshGraph();
        },
        unPinCurrentEdge: function() {
          this.scope.wfPinnedEdge = undefined;
          this.refreshGraph();
        },
        isEdgePinned: function(from, to) {
          if (this.scope.wfPinnedEdge && this.scope.wfPinnedEdge.from === from && this.scope.wfPinnedEdge.to === to) {
            return true;
          } else {
            return false;
          }
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
          this.scope.$digest();
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
          return (this.scope.pinnedWorkflowStep && stepId === this.scope.pinnedWorkflowStep.name);
        },
        hasStepPinned: function() {
          return this.scope.pinnedWorkflowStep;
        },
        clearSelection: function() {
          this.scope.workflowCurrentStepSelection = [];
          this.scope.pinnedWorkflowStep = undefined;
          this.scope.previewWorkflowStep = undefined;
          this.scope.wfPinnedEdge = undefined;
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

          this.refreshGraph(true, true);
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
          this.scope.$digest();
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
          var oldWorkflows = Object.keys(scope.topology.topology.workflows);
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.workflow.CreateWorkflowOperation',
              workflowName: 'customWorkflow'
            },
            function(successResult) {
              if (!successResult.error) {
                instance.clearSelection();
                // compare the old workflow keys list with the new one
                // and so guess the new workflow name
                var newWorkflows = Object.keys(scope.topology.topology.workflows);
                var addedWorkflows = _.difference(newWorkflows, oldWorkflows);
                if (addedWorkflows.length === 1) {
                  instance.setCurrentWorkflowName(addedWorkflows[0]);
                }
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
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.workflow.RemoveWorkflowOperation',
              workflowName: scope.currentWorkflowName
            },          
            function(successResult) {
              if (!successResult.error) {
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
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.workflow.RenameWorkflowOperation',
              workflowName: scope.currentWorkflowName,
              newName: newName
            },           
            function(successResult) {
              if (!successResult.error) {
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
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.workflow.ReinitializeWorkflowOperation',
              workflowName: scope.currentWorkflowName
            },           
            function(successResult) {
              if (!successResult.error) {
                instance.refreshGraph(true, true);
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
          this.scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.workflow.RenameStepOperation',
            stepId: stepId,
            workflowName: scope.currentWorkflowName,
            newName: newStepName
          },
          function(successResult) {
              if (_.undefined(successResult.error)) {
                instance.refreshGraph(true, true);
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
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.workflow.RemoveEdgeOperation',
              fromStepId: from,
              toStepId: to,
              workflowName: scope.currentWorkflowName
            },          
            function(successResult) {
              if (!successResult.error) {
                if (scope.pinnedWorkflowStep) {
                  instance.setPinnedWorkflowStep(scope.pinnedWorkflowStep.name, scope.topology.topology.workflows[scope.currentWorkflowName].steps[scope.pinnedWorkflowStep.name]);
                }
                if (instance.isEdgePinned(from, to)) {
                  scope.wfPinnedEdge = undefined;
                }
                instance.refreshGraph(true, true);
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
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.workflow.RemoveStepOperation',
              stepId: stepId,
              workflowName: scope.currentWorkflowName
            },          
            function(successResult) {
              if (!successResult.error) {
                instance.clearSelection();
                instance.refreshGraph(true, true);
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
          if (connectFromCandidate.length === 0) {
            return;
          }
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.workflow.ConnectStepFromOperation',
              toStepId: scope.pinnedWorkflowStep.name,
              workflowName: scope.currentWorkflowName,
              fromStepIds: connectFromCandidate
            },            
            function(successResult) {
              if (!successResult.error) {
                if (scope.pinnedWorkflowStep) {
                  instance.setPinnedWorkflowStep(scope.pinnedWorkflowStep.name, scope.topology.topology.workflows[scope.currentWorkflowName].steps[scope.pinnedWorkflowStep.name]);
                }
                instance.refreshGraph(true, true);
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
          if (connectToCandidate.length === 0) {
            return;
          }
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.workflow.ConnectStepToOperation',
              fromStepId: scope.pinnedWorkflowStep.name,
              workflowName: scope.currentWorkflowName,
              toStepIds: connectToCandidate
            },
            function(successResult) {
              if (!successResult.error) {
                if (scope.pinnedWorkflowStep) {
                  instance.setPinnedWorkflowStep(scope.pinnedWorkflowStep.name, scope.topology.topology.workflows[scope.currentWorkflowName].steps[scope.pinnedWorkflowStep.name]);
                }
                instance.refreshGraph(true, true);
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
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.workflow.SwapStepOperation',
              stepId: from,
              workflowName: scope.currentWorkflowName,
              targetStepId: to
            },          
            function(successResult) {
              if (!successResult.error) {
                instance.refreshGraph(true, true);
                if (scope.pinnedWorkflowStep) {
                  instance.setPinnedWorkflowStep(scope.pinnedWorkflowStep.name, scope.topology.topology.workflows[scope.currentWorkflowName].steps[scope.pinnedWorkflowStep.name]);
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
              type: 'org.alien4cloud.tosca.editor.operations.workflow.AddActivityOperation',
              workflowName: scope.currentWorkflowName,
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

          this.scope.execute(activityRequest, 
            function(successResult) {
              if (!successResult.error) {
                var wf = successResult.data;
                if (scope.pinnedWorkflowStep) {
                  instance.setPinnedWorkflowStep(scope.pinnedWorkflowStep.name, scope.topology.topology.workflows[scope.currentWorkflowName].steps[scope.pinnedWorkflowStep.name]);
                }
                instance.refreshGraph(true, true);
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
              type: 'org.alien4cloud.tosca.editor.operations.workflow.AddActivityOperation',
              workflowName: scope.currentWorkflowName,
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
        refreshGraph: function(layout, center) {
          this.scope.$broadcast('WfRefresh', {layout: layout, center: center});
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
          } else if (shortType === 'DelegateWorkflowActivity') {
            return '\uf011'; // fa-power-off
          } else if (shortType === 'SetStateActivity') {
            if (this.stateIcon[step.activity.stateName]) {
              return this.stateIcon[step.activity.stateName];
            } else {
              return '\uf087'; // fa-thumbs-o-up
            }
          } else {
            return '\uf1e2'; // fa-bomb
          }
        },
        getStepActivityType: function(step) {
          if (step.activity) {
            return $filter('splitAndGet')(step.activity.type, '.', 'last');
          } else {
            return undefined;
          }
        },
        getStepActivityDetails: function(step) {
          var details = {};
          var shortType = this.getStepActivityType(step);
          if (shortType === 'OperationCallActivity') {
            details.interfaceName = step.activity.interfaceName;
            details.operationName = step.activity.operationName;
          } else if (shortType === 'SetStateActivity') {
            details.stateName = step.activity.stateName;
          } else if (shortType === 'DelegateWorkflowActivity') {
            details.delegateWorkflow = step.activity.workflowName;
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
                var lastStep;
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
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.workflows = instance;
      };
    }
  ]); // modules
}); // define
