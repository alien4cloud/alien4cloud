define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-orchestrators', ['ui.bootstrap']).controller('NewOrchestratorController', ['$scope', '$uibModalInstance', '$http',
    function($scope, $uibModalInstance, $http) {
      $scope.newOrchestrator = {};
      // get the list of orchestrator plugins
      $http.get('rest/latest/plugincomponents?type=IOrchestratorPluginFactory').then(function(response) {
        if(_.defined(response.data.data)) {
          $scope.plugins = response.data.data;
          for (var i = 0; i < $scope.plugins.length; i++) {
            $scope.plugins[i].nameAndId = $scope.plugins[i].componentDescriptor.name + ' : ' + $scope.plugins[i].version;
          }
        }
      });

      $scope.save = function(valid) {
        if (valid) {
          $uibModalInstance.close($scope.newOrchestrator);
        }
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ]);
});
