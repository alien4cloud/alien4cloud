define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators', ['ui.bootstrap']).controller('NewLocationController', ['$scope', '$modalInstance', 'locationTypes',
    function($scope, $modalInstance, locationTypes) {
      $scope.newLocation = {};
      $scope.locationTypes = locationTypes;

      $scope.save = function(valid) {
        if (valid) {
          console.log('closing', $scope.newLocation);
          $modalInstance.close($scope.newLocation);
        }
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]);
});
