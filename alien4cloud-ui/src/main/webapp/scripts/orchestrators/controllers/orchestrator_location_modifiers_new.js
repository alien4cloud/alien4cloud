define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators', ['ui.bootstrap']).controller('LocationModifierNewController', ['$scope', '$uibModalInstance', 'modifierReference',
    function($scope, $uibModalInstance, modifierReference) {
      $scope.flowPhases = [
        'post-location-match',
        'pre-inject-input',
        'post-inject-input',
        'pre-policy-match',
        'post-policy-match',
        'pre-node-match',
        'post-node-match',
        'pre-matched-policy-setup',
        'post-matched-policy-setup',
        'pre-matched-node-setup',
        'post-matched-node-setup' ];

      $scope.modifierReference = modifierReference;

      $scope.save = function(valid) {
        if (valid) {
          $uibModalInstance.close($scope.modifierReference);
        }
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ]);
});
