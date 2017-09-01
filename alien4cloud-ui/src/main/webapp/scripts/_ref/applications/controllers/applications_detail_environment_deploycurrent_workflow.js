define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/topology/controllers/topology_editor_workflows');
  require('scripts/topology/directives/workflow_rendering');

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
    ['$scope', 'topoEditDisplay', 'topoEditWf', 'applicationServices', 'breadcrumbsService', '$translate', '$state',
      function ($scope, topoEditDisplay, topoEditWf, applicationServices, breadcrumbsService, $translate, $state) {

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

        $scope.launchWorkflow = function () {
          $scope.isLaunchingWorkflow = true;
          applicationServices.launchWorkflow({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.selectedEnvironment.id,
            workflowName: $scope.currentWorkflowName
          }, undefined, function success() {
            $scope.isLaunchingWorkflow = false;
          });
        };
      }]);
});
