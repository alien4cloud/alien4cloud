define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/topology/controllers/topology_editor_workflows');
  require('scripts/topology/directives/workflow_rendering');
  require('scripts/_ref/applications/services/secret_display_modal');
  require('scripts/applications/services/workflow_execution_services');


  states.state('applications.detail.environment.deploycurrent.workflow', {
    url: '/workflow',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_workflow.html',
    controller: 'ApplicationEnvDeployCurrentWorkflowCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.workflow',
      state: 'applications.detail.environment.deploycurrent.workflow',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT_WORKFLOW_VIEWER',
      icon: 'fa fa-code-fork fa-rotate-90',
      priority: 300
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentWorkflowCtrl',
    ['$scope', 'topoEditDisplay', 'topoEditWf', 'topoEditProperties', 'applicationServices', 'workflowExecutionServices', 'breadcrumbsService', '$translate', '$state', 'secretDisplayModal', 'toaster', '$timeout',
      function ($scope, topoEditDisplay, topoEditWf, topoEditProperties, applicationServices, workflowExecutionServices, breadcrumbsService, $translate, $state, secretDisplayModal, toaster, $timeout) {
        breadcrumbsService.putConfig({
          state : 'applications.detail.environment.deploycurrent.workflow',
          text: function(){
            return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_CURRENT_WORKFLOW_VIEWER');
          },
          onClick: function(){
            $state.go('applications.detail.environment.deploycurrent.workflow');
          }
        });

        $scope.wfViewMode = 'simple';
        $scope.displays = {
          workflows: { active: true, size: 400, selector: '#workflow-menu-box', only: ['workflows'] }
        };
        topoEditDisplay($scope, '#workflow-graph');
        topoEditWf($scope);

        topoEditProperties($scope);

        // set wf in 'runtime' mode ie. don't fill nodes when selected but regarding step states
        $scope.workflows.setEditorMode('runtime');
        $scope.isWaitingForMonitoringRefresh = false;

        $scope.$on('a4cRuntimeTopologyLoaded', function () {
          $scope.workflows.setCurrentWorkflowName('install');
        });
        $scope.workflows.setCurrentWorkflowName('install');

        applicationServices.getActiveMonitoredDeployment.get({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.environment.id
        }, undefined, function(success) {
          if (_.defined(success.data)) {
            $scope.activeDeployment = success.data.deployment;
            $scope.refreshWorkflowMonitoring();
          }
        });

        $scope.$on('a4cRuntimeEventReceived', function(angularEvent, event) {
          $timeout(function() {
            $scope.refreshWorkflowMonitoring();
          }, 200);
        });

        applicationServices.getSecretProviderConfigurationsForCurrentDeployment.get({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.environment.id
        }, undefined, function(success) {
          if (_.defined(success.data)) {
            $scope.secretProviderConfigurations = success.data;
          }
        });

        $scope.displayLogs = function (task) {
          $state.go('applications.detail.environment.deploycurrent.logs', {
            'applicationId': $scope.application.id,
            'applicationEnvironmentId': $scope.environment.id,
            'taskId': task.id,
            'executionId': $scope.workflowExecutionMonitoring.execution.id
          });
        },

        $scope.refreshWorkflowMonitoring = function () {
          if ($scope.isWaitingForMonitoringRefresh) {
            return;
          }
          $scope.isWaitingForMonitoringRefresh = true;
          workflowExecutionServices.get({
            deploymentId: $scope.activeDeployment.id
          }, function (result) {
            $scope.isWaitingForMonitoringRefresh = false;
            $scope.workflowExecutionMonitoring = result.data;

            $scope.workflows.setCurrentWorkflowName(result.data.execution.workflowName);

            if ($scope.workflowExecutionMonitoring.execution.id === $scope.currentWorkflowExecutionId) {
              // a wf has been launched from menu
              if ($scope.workflowExecutionMonitoring.execution.status !== 'RUNNING') {
                    $scope.currentWorkflowExecutionId = null;
                    $scope.isLaunchingWorkflow = false;

                    var popup_qualifier = undefined;
                    var popup_status = undefined;
                    if ($scope.workflowExecutionMonitoring.execution.status === 'SUCCEEDED') {
                        popup_qualifier = 'SUCCESS';
                        popup_status = 'success';
                    } else if ($scope.workflowExecutionMonitoring.execution.status === 'FAILED') {
                        popup_qualifier = 'FAIL';
                        popup_status = 'error'
                    }
                    if (popup_qualifier) {
                        var title = $translate.instant('APPLICATIONS.RUNTIME.WORKFLOW.' + popup_qualifier + '_TITLE', {
                            'workflowId': result.data.execution.workflowName
                        });
                        toaster.pop(popup_status, title, '', 0, 'trustedHtml', null);
                    }
                }
            } else {
                if ($scope.workflowExecutionMonitoring.execution.status === 'RUNNING') {
                    $scope.isLaunchingWorkflow = true;
                } else {
                    $scope.isLaunchingWorkflow = false;
                }
            }

            $scope.workflows.refreshGraph(true, true);
          }, function(error) {
            $scope.isWaitingForMonitoringRefresh = false;
            console.log("No workflow execution found");
          });
        };

        $scope.launchWorkflow = function () {
          secretDisplayModal($scope.secretProviderConfigurations).then(function (secretProviderInfo) {
            var request = {};
            if (_.defined(secretProviderInfo)) {
              request.secretProviderConfiguration = $scope.secretProviderConfigurations[0];
              request.credentials = secretProviderInfo.credentials;
            }

            request.inputs = $scope.workflowInputsValues;

            $scope.isLaunchingWorkflow = true;
            $scope.currentWorkflowExecutionId = null;
            applicationServices.launchWorkflow({
              applicationId: $scope.application.id,
              applicationEnvironmentId: $scope.environment.id,
              workflowName: $scope.currentWorkflowName
            }, angular.toJson(request), function success(response) {
              var resultHtml = [];
              var title = '';
              if (_.defined(response.error)) {
                title = $translate.instant('ERRORS.' + response.error.code + '.TITLE');
                var msgHtml = $translate.instant('ERRORS.' + response.error.code + '.MESSAGE', {
                  'workflowId': $scope.currentWorkflowName
                });
                resultHtml.push('<li>' + msgHtml + '</li>');
                toaster.pop('error', title, resultHtml.join(''), 0, 'trustedHtml', null);
                $scope.isLaunchingWorkflow = false;
              } else {
                $scope.isLaunchingWorkflow = true;
                $scope.currentWorkflowExecutionId = response.data;
              }
            });

          });
        };
      }]);
});
