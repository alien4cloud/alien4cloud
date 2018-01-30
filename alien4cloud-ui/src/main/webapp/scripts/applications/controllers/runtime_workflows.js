// Editor workflow editor controller.
// Editor file browser controller.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/topology/controllers/topology_editor_workflows');
  require('scripts/topology/directives/workflow_rendering');

  states.state('applications.detail.runtime.workflow', {
    url: '/workflow',
    templateUrl: 'views/topology/topology_runtime_workflows.html',
    controller: 'TopologyWorkflowRuntimeCtrl',
    menu: {
      id: 'am.applications.detail.runtime.workflow',
      state: 'applications.detail.runtime.workflow',
      key: 'EDITOR.MENU_WORKFLOW',
      icon: 'fa fa-code-fork fa-rotate-90',
      priority: 2
    }
  });

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('TopologyWorkflowRuntimeCtrl',
    ['$scope', 'topoEditDisplay', 'topoEditWf', 'applicationServices', 'toaster', '$translate',
    function($scope, topoEditDisplay, topoEditWf, applicationServices, toaster, $translate) {
    $scope.displays = {
      workflows: { active: true, size: 400, selector: '#workflow-menu-box', only: ['workflows'] }
    };
    topoEditDisplay($scope, '#workflow-graph');
    topoEditWf($scope);

    $scope.$on('a4cRuntimeTopologyLoaded', function() {
      $scope.workflows.setCurrentWorkflowName('install');
    });
    $scope.workflows.setCurrentWorkflowName('install');

    $scope.launchWorkflow = function() {
      $scope.isLaunchingWorkflow = true;
      applicationServices.launchWorkflow({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.selectedEnvironment.id,
        workflowName: $scope.currentWorkflowName
      }, undefined, function success(response) {
        if (_.defined(response.error)) {
          var title = $translate.instant('ERRORS.' + response.error.code + '.TITLE');
          var resultHtml = [];
          var msgHtml = $translate.instant('ERRORS.' + response.error.code + '.MESSAGE', {
            'workflowId': $scope.currentWorkflowName
          });
          resultHtml.push('<li>' + msgHtml + '</li>');
          toaster.pop('error', title, resultHtml.join(''), 0, 'trustedHtml', null);
          $scope.isLaunchingWorkflow = false;
        } else {
          var title = $translate.instant("APPLICATIONS.RUNTIME.WORKFLOW.SUCCESS_TITLE", {'workflowId': $scope.currentWorkflowName});
          var resultHtml = [];
          toaster.pop('success', title, resultHtml.join(''), 0, 'trustedHtml', null);
          $scope.isLaunchingWorkflow = false;
        }
      });
    };
  }]);
});
