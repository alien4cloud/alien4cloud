'use strict';

angular.module('alienUiApp').controller(
  'NewCloudImageController', [
    '$scope',
    '$modalInstance',
    'cloudImageServices',
    function($scope, $modalInstance, cloudImageServices) {

      $scope.cloudImageFormDescriptor = cloudImageServices.getFormDescriptor();

      $scope.cloudImage = {};

      $scope.save = function() {
        $modalInstance.close($scope.cloudImage);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]
);
