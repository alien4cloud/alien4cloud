/* global UTILS */
'use strict';

angular.module('alienUiApp').controller(
  'NewCloudImageController', [ '$scope', '$modalInstance', 'cloudImageServices','$q',
    function($scope, $modalInstance, cloudImageServices, $q) {
      $scope.cloudImageFormDescriptor = cloudImageServices.getFormDescriptor();
      $scope.cloudImage = {};

      $scope.save = function(cloudImage) {
        return cloudImageServices.create({}, angular.toJson(cloudImage), function success(response) {
          if (UTILS.isDefinedAndNotNull(response.error)) {
            var errorsHandle = $q.defer();
            return errorsHandle.resolve(response.error);
          } else {
            $modalInstance.close(response.data);
          }
        }).$promise;
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]
);
