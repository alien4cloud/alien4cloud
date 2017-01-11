define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators', ['ui.bootstrap']).controller('NewLocationController', ['$scope', '$uibModalInstance', 'locationTypes',
    function($scope, $uibModalInstance, locationTypes) {
      $scope.newLocation = {};
      $scope.locationTypes = locationTypes;

      $scope.save = function(valid) {
        if (valid) {
          $uibModalInstance.close($scope.newLocation);
        }
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ]);
});
