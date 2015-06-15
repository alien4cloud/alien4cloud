'use strict';

angular.module('alienUiApp').controller(
  'NewNetworkController', [
    '$scope',
    '$modalInstance',
    'cloudServices',
    function($scope, $modalInstance, cloudServices) {

      $scope.networkFormDescriptor = cloudServices.networkFormDescriptor;

      $scope.network = {};

      $scope.save = function() {
        $modalInstance.close($scope.network);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]
);
