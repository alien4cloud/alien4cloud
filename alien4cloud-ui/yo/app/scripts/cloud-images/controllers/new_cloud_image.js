'use strict';

angular.module('alienUiApp').controller(
  'NewCloudImageController', [
    '$scope',
    '$modalInstance',
    'cloudImageServices',
    function($scope, $modalInstance, cloudImageServices) {

      $scope.cloudImageFormDescriptor = cloudImageServices.getFormDescriptor();

      $scope.cloudImage = {};

      $scope.save = function(cloudImage) {
        return cloudImageServices.create({}, angular.toJson(cloudImage)).$promise.then(function(success){
          $modalInstance.close(success.data);
        });
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]
);
