// Editor workflow editor controller.
// Editor file browser controller.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('angular-ui-ace');

  require('angular-tree-control');
  require('scripts/common/services/explorer_service');

  require('scripts/topology/controllers/topology_editor_workflows');

  require('scripts/topology/directives/workflow_rendering');
  require('scripts/topology/directives/topology_rendering');
  require('scripts/topology/controllers/workflow_operation_selector');
  require('scripts/topology/controllers/workflow_state_selector');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('TopologyWorkflowEditorCtrl',
    ['$scope', '$http', 'explorerService', '$stateParams', 'topoEditDisplay', 'topoEditWf',
    function($scope, $http, explorerService, $stateParams, topoEditDisplay, topoEditWf) {
    $scope.displays = {
      workflows: { active: true, size: 400, selector: '#workflow-menu-box', only: ['workflows'] },
    };
    topoEditDisplay($scope, '#workflow-graph');
    topoEditWf($scope);

    $scope.workflows.setCurrentWorkflowName('install');

    // Load archive content file
    $scope.$on('topologyRefreshedEvent', function() {

    });
    if(_.defined($scope.topology)) {

    }
  }]);
});
