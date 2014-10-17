'use strict';

angular.module('alienUiApp').controller(
  'NewCloudImageFlavorController', [
    '$scope',
    '$modalInstance',
    'cloudServices',
    function($scope, $modalInstance, cloudServices) {

      $scope.flavorFormDescriptor = cloudServices.flavorFormDescriptor;

      $scope.flavor = {};

      $scope.save = function() {
        $modalInstance.close($scope.flavor);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]
);
