// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('a4cEditorInputExpEditCtrl',
    ['$scope', '$uibModal', 'inputName', 'inputExpression', '$uibModalInstance',
      function($scope,  $uibModal, inputName, inputExpression, $uibModalInstance) {
        $scope.inputName = inputName;
        $scope.inputExpression = inputExpression;
        $scope.ok = function() {
          $uibModalInstance.close($scope.inputExpression);
        };

        $scope.cancel = function() {
          $uibModalInstance.dismiss('canceled');
        };
      }
    ]);
});
