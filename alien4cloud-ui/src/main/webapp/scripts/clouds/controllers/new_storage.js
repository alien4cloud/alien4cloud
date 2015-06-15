'use strict';

angular.module('alienUiApp').controller(
  'NewStorageController', [
    '$scope',
    '$modalInstance',
    'cloudServices',
    function($scope, $modalInstance, cloudServices) {

      $scope.storageFormDescriptor = cloudServices.storageFormDescriptor;

      $scope.storage = {};

      $scope.save = function() {
        $modalInstance.close($scope.storage);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]
);
