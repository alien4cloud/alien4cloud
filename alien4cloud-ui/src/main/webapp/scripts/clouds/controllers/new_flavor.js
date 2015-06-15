define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-clouds', ['ui.bootstrap']).controller(
    'NewCloudImageFlavorController', ['$scope', '$modalInstance', 'cloudServices',
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
  ]); // controller
}); // define
