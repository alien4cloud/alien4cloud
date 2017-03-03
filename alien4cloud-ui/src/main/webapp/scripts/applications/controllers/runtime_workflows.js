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
    ['$scope', 'topoEditDisplay', 'topoEditWf', 'applicationServices',
    function($scope, topoEditDisplay, topoEditWf, applicationServices) {
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
      }, undefined, function success() {
        $scope.isLaunchingWorkflow = false;
      });
    };
  }]);
});
