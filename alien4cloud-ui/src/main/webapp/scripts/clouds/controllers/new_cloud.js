// list of cloud images that can be defined for multiple clouds actually.
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-clouds', ['ui.bootstrap']).controller('NewCloudController', ['$scope', '$modalInstance', '$http',
    function($scope, $modalInstance, $http) {
      $scope.newCloud = {};
      $http.get('rest/passprovider').success(function(response) {
        $scope.paasProviders = response.data || {};
        for (var i = 0; i < $scope.paasProviders.length; i++) {
          $scope.paasProviders[i].nameAndId = $scope.paasProviders[i].componentDescriptor.name + " : " + $scope.paasProviders[i].version;
        }
      });

      $scope.save = function(valid) {
        if (valid) {
          $modalInstance.close($scope.newCloud);
        }
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]);
});
