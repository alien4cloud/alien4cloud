'use strict';

angular.module('alienUiApp').controller(
  'NewCloudController', ['$scope', '$modalInstance', '$http', function($scope, $modalInstance, $http) {
  $scope.newCloud = {};

  $http.get('rest/passprovider').success(function(response) {
    $scope.paasProviders = response.data;
  });

  $scope.save = function(valid) {
    if(valid) {
      $modalInstance.close($scope.newCloud);
    }
  };

  $scope.cancel = function() {
    $modalInstance.dismiss('cancel');
  };
}]);
