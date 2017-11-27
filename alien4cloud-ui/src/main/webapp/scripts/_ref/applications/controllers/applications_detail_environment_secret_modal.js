define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications', ['ui.bootstrap']).controller('SecretCredentialsController', ['$scope', '$uibModalInstance', 'secretCredentialInfos',
    function ($scope, $uibModalInstance, secretCredentialInfos) {
      $scope.pluginName = secretCredentialInfos[0].pluginName;
      $scope.pluginConfigurationDescriptor = secretCredentialInfos[0].credentialDescriptor;
      $scope.pluginConfigurationValues = {};

      $scope.ok = function () {
        $uibModalInstance.close({
          pluginName: $scope.pluginName,
          credentials: $scope.pluginConfigurationValues
        });
      };

      $scope.cancel = function () {
        $uibModalInstance.close({
          pluginName: $scope.pluginName
        });
      };
    }
  ]);
});
