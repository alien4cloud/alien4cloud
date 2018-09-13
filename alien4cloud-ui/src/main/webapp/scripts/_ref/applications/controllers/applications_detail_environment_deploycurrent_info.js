define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var alienUtils = require('scripts/utils/alien_utils');
  require('scripts/deployment/directives/display_outputs');

  states.state('applications.detail.environment.deploycurrent.info', {
    url: '/info',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_info.html',
    controller: 'ApplicationEnvDeployCurrentInfoCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.info',
      state: 'applications.detail.environment.deploycurrent.info',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT_INFO',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentInfoCtrl',
  ['$scope', 'applicationServices', 'workflowExecutionServices', 'application', '$state','breadcrumbsService', '$translate',
  function($scope, applicationServices, workflowExecutionServices, applicationResult, $state, breadcrumbsService, $translate) {

    breadcrumbsService.putConfig({
      state : 'applications.detail.environment.deploycurrent.info',
      text: function(){
        return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_CURRENT_INFO');
      },
      onClick: function(){
        $state.go('applications.detail.environment.deploycurrent.info');
      }
    });

    $scope.applicationServices = applicationServices;
    $scope.fromStatusToCssClasses = alienUtils.getStatusIconCss;
    $scope.getExecutionStatusCss = alienUtils.getExecutionStatusCss;
    $scope.application = applicationResult.data;
    $scope.isWaitingForMonitoringRefresh = false;
    $scope.wfProgressData = undefined;
    // here are the step instance expected count per workflow
    $scope.monitoredWorkflowExpectedStepInstanceCount = undefined;

    applicationServices.getActiveMonitoredDeployment.get({
      applicationId: $scope.application.id,
      applicationEnvironmentId: $scope.environment.id
    }, undefined, function(success) {
      if (_.defined(success.data)) {
        $scope.activeDeployment = success.data.deployment;
        $scope.monitoredWorkflowExpectedStepInstanceCount = success.data.workflowExpectedStepInstanceCount;
        $scope.deployedTime = new Date() - $scope.activeDeployment.startDate;
        $scope.refreshWorkflowMonitoring();
      }
    });

    $scope.$on('a4cRuntimeEventReceived', function(angularEvent, event) {
      $scope.refreshWorkflowMonitoring();
    });

    $scope.refreshWorkflowMonitoring = function () {
      if ($scope.isWaitingForMonitoringRefresh) {
        return;
      }
      $scope.isWaitingForMonitoringRefresh = true;
      workflowExecutionServices.get({
        deploymentId: $scope.activeDeployment.id
      }, function (result) {
        $scope.isWaitingForMonitoringRefresh = false;
        var workflowName = result.data.execution.workflowName;
        if ($scope.monitoredWorkflowExpectedStepInstanceCount.hasOwnProperty(workflowName)) {
          // always add +1 to the total to avoid false full bar (the total is an estimation)
          var progress = (result.data.actualKnownStepInstanceCount * 100) / $scope.monitoredWorkflowExpectedStepInstanceCount[workflowName];
          if (result.data.execution.status === 'SUCCEEDED') {
              progress = 100;
          } else {
              if (progress >= 95) {
                  progress = 90;
              }
          }
          $scope.wfProgressData = {'workflowName': workflowName, 'progress': progress, 'status': result.data.execution.status, 'current': result.data.lastKnownExecutingTask};
        }
      }, function(error) {
        $scope.isWaitingForMonitoringRefresh = false;
      });
    };

    $scope.displayWorkflows = function() {
      $state.go('applications.detail.environment.deploycurrent.workflow');
    };

  }
]);
});
