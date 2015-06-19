define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-clouds', ['ui.bootstrap']).controller(
    'NewNetworkController', ['$scope', '$modalInstance', 'cloudServices',
    function($scope, $modalInstance, cloudServices) {
      $scope.networkFormDescriptor = cloudServices.networkFormDescriptor;

      $scope.network = {};

      $scope.save = function() {
        $modalInstance.close($scope.network);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]); // controller
}); // define
