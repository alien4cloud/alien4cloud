define(function (require) {
  'use strict';
  
  var modules = require('modules');
  var _ = require('lodash');
  
  modules.get('a4c-applications', ['ui.bootstrap']).controller('SecretCredentialsController', ['$scope', '$uibModalInstance',
    function ($scope, $uibModalInstance) {
      $scope.pluginName = $scope.deploymentTopologyDTO.secretCredentialInfos[0].pluginName;
      $scope.ok = function (valid) {
        console.log(valid);
        $uibModalInstance.close();
      };
      
      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ]);
});
