define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators', ['ui.bootstrap']).controller('NewOrchestratorController', ['$scope', '$modalInstance', '$http',
    function($scope, $modalInstance, $http) {
      $scope.newOrchestrator = {};
      // get the list of orchestrator plugins
      $http.get('rest/plugincomponents?type=IOrchestratorPluginFactory').success(function(response) {
        $scope.plugins = response.data || [];
        for (var i = 0; i < $scope.plugins.length; i++) {
          $scope.plugins[i].nameAndId = $scope.plugins[i].componentDescriptor.name + ' : ' + $scope.plugins[i].version;
        }
      });

      $scope.save = function(valid) {
        if (valid) {
          $modalInstance.close($scope.newOrchestrator);
        }
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]);
});
