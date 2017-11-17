define(function (require) {
  'use strict';
  
  var modules = require('modules');
  var _ = require('lodash');
  
  modules.get('a4c-applications', ['ui.bootstrap']).controller('SecretCredentialsController', ['$scope', '$uibModalInstance',
    function ($scope, $uibModalInstance) {
      $scope.pluginName = $scope.deploymentTopologyDTO.secretCredentialInfos[0].pluginName;
      $scope.pluginConfigurationDescriptor = $scope.deploymentTopologyDTO.secretCredentialInfos[0].credentialDescriptor;
      $scope.pluginConfigurationValues = {};

      $scope.ok = function () {
        $uibModalInstance.close({
          pluginName: $scope.pluginName,
          credentials: $scope.pluginConfigurationValues
        });
      };
      
      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ]);
});
