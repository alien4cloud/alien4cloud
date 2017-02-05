define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-services', ['ui.bootstrap']).controller('a4cNewServiceResourceCtrl', ['$scope', '$uibModalInstance',
    function($scope, $uibModalInstance) {
      $scope.save = function(valid) {
        if (valid) {
          $uibModalInstance.close($scope.createServiceRequest);
        }
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ]);
});
