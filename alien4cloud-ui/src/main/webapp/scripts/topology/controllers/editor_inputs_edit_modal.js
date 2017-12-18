// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var yaml = require('js-yaml');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('a4cEditorInputExpEditCtrl',
    ['$scope', '$uibModal', 'inputName', 'inputExpression', '$uibModalInstance',
      function($scope,  $uibModal, inputName, inputExpression, $uibModalInstance) {
        $scope.inputName = inputName;

        $scope.inputExpression = {
          str: inputExpression,
          obj: yaml.safeLoad(inputExpression)
        };

        $scope.getPropertyDefinition = function() {
          return $scope.topology.topology.inputs[inputName];
        };

        $scope.updateLocalExpression = function(def, name, value) {
          $scope.inputExpression.str = yaml.safeDump(value, {indent: 4});
        };

        $scope.ok = function() {
          $uibModalInstance.close($scope.inputExpression);
        };

        $scope.cancel = function() {
          $uibModalInstance.dismiss('canceled');
        };
      }
    ]);
});
