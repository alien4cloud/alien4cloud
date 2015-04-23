'use strict';

angular.module('alienUiApp').controller(
  'NewZoneController', [
    '$scope',
    '$modalInstance',
    'cloudServices',
    function($scope, $modalInstance, cloudServices) {

      $scope.zoneFormDescriptor = cloudServices.zoneFormDescriptor;

      $scope.zone = {};

      $scope.save = function() {
        $modalInstance.close($scope.zone);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]
);
