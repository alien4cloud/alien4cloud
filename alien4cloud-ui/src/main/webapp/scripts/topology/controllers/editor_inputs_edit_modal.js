// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var yaml = require('js-yaml');
  var angular = require('angular');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('a4cEditorInputExpEditCtrl',
    ['$scope', '$uibModal', 'inputName', 'inputExpression', '$uibModalInstance', 'propertiesServices',
      function($scope,  $uibModal, inputName, inputExpression, $uibModalInstance, propertiesServices) {
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

        $scope.typeMatch = false;
        if(_.definedPath($scope, 'inputExpression.obj')){
          propertiesServices.validConstraints({}, angular.toJson({
            'definitionId': $scope.inputName,
            'propertyDefinition': $scope.getPropertyDefinition(),
            'dependencies': $scope.topology.topology.dependencies,
            'value': $scope.inputExpression.obj
          }), function(successResult) {
            if(_.get(successResult, 'error.code') === 804) {
              $scope.typeMatch = false;
              $scope.activeTab = 1;
              return;
            }
            $scope.typeMatch = true;
          });
        }
      }
    ]);
});
