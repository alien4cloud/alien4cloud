define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-clouds', ['ui.bootstrap']).controller(
  'NewStorageController', ['$scope', '$modalInstance', 'cloudServices',
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
  ]); // controller
}); // define
