define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/topology/controllers/topology_editor_workflows');
  require('scripts/topology/directives/workflow_rendering');
  require('scripts/_ref/applications/services/secret_display_modal');

  states.state('applications.detail.environment.deploycurrent.workflow', {
    url: '/runtime_editor',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_workflow.html',
    controller: 'ApplicationEnvDeployCurrentWorkflowCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.workflow',
      state: 'applications.detail.environment.deploycurrent.workflow',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.WORKFLOW_VIEWER',
      icon: 'fa fa-code-fork fa-rotate-90',
      priority: 300
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentWorkflowCtrl',
    ['$scope', 'topoEditDisplay', 'topoEditWf', 'applicationServices', 'breadcrumbsService', '$translate', '$state', 'secretDisplayModal',
      function ($scope, topoEditDisplay, topoEditWf, applicationServices, breadcrumbsService, $translate, $state, secretDisplayModal) {

        breadcrumbsService.putConfig({
          state : 'applications.detail.environment.deploycurrent.workflow',
          text: function(){
            return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.WORKFLOW_VIEWER');
          },
          onClick: function(){
            $state.go('applications.detail.environment.deploycurrent.workflow');
          }
        });

        $scope.displays = {
          workflows: { active: true, size: 400, selector: '#workflow-menu-box', only: ['workflows'] }
        };
        topoEditDisplay($scope, '#workflow-graph');
        topoEditWf($scope);

        $scope.$on('a4cRuntimeTopologyLoaded', function () {
          $scope.workflows.setCurrentWorkflowName('install');
        });
        $scope.workflows.setCurrentWorkflowName('install');

        applicationServices.getSecretProviderConfigurationsForCurrentDeployment.get({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.environment.id
        }, undefined, function(success) {
          if (_.defined(success.data)) {
            $scope.secretProviderConfigurations = success.data;
          }
        });

        $scope.launchWorkflow = function () {
          secretDisplayModal($scope.secretProviderConfigurations).then(function (secretProviderInfo) {
            var secretProviderInfoRequest = {};
            if (_.defined(secretProviderInfo)) {
              secretProviderInfoRequest.secretProviderConfiguration = $scope.secretProviderConfigurations[0];
              secretProviderInfoRequest.credentials = secretProviderInfo.credentials;
            }

            $scope.isLaunchingWorkflow = true;
            applicationServices.launchWorkflow({
              applicationId: $scope.application.id,
              applicationEnvironmentId: $scope.environment.id,
              workflowName: $scope.currentWorkflowName
            }, angular.toJson(secretProviderInfoRequest), function success() {
              $scope.isLaunchingWorkflow = false;
            });

          });
        };
      }]);
});
