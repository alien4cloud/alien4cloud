define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('angular-ui-ace');

  require('angular-tree-control');

  modules.get('a4c-topology-editor').controller('WfInlineSelectorController', [
    '$scope', '$uibModalInstance', 'topologyDTO', 'thisWorkflow',
    function ($scope, $uibModalInstance, topologyDTO, thisWorkflow) {

      function getWorkflowItems() {
        var allWorkflowItems = Object.keys(topologyDTO.topology.workflows);
        _.remove(allWorkflowItems, function (currentWorkflowItem) {
          return currentWorkflowItem === thisWorkflow;
        });
        return allWorkflowItems;
      }

      $scope.workflowItems = getWorkflowItems();
      $scope.selectedWorkflow = undefined;

      $scope.submit = function () {
        $uibModalInstance.close($scope.selectedWorkflow);
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ]);
});
