define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-clouds', ['ui.bootstrap']).controller(
    'NewZoneController', ['$scope', '$modalInstance', 'cloudServices',
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
  ]); // controller
}); // define
