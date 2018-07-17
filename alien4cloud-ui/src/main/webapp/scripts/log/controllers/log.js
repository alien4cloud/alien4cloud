define(function (require) {
  'use strict';
  var modules = require('modules');

  require('scripts/applications/services/application_services');

  modules.get('alien4cloud-premium-logs', ['ui.bootstrap']).controller('LogController',
  ['$scope', 'applicationServices', '$state', '$filter',
    function ($scope,applicationServices, $state, $filter) {
      if($state.is('applications.detail.environment.deploycurrent.logs')) {
        applicationServices.getActiveDeployment.get({
          applicationId: $state.params.id,
          applicationEnvironmentId: $state.params.environmentId
        }, function(result) {
          $scope.deploymentId = result.data.id;
        });
      } else {
        $scope.deploymentId = $state.params.deploymentId;
      }

      $scope.instanceId = $state.params.instanceId;
      $scope.executionId = $state.params.executionId;
      $scope.taskId = $state.params.taskId;

      $scope.updateSearchResult = function (searchResult) {
        $scope.searchResult = searchResult;
      };

      $scope.searchConfig = {};

      var timestampFormat = 'yyyy-MM-dd HH:mm:ss';
      function formatDate (logs) {
        if(logs){
          logs.forEach(function (log) {
            log.timestamp = $filter('date')(log.timestamp, timestampFormat);
          });
        }
      }

      $scope.onSearch = function (searchConfig) {
        $scope.searchConfig.result = searchConfig.result;
        // This is used by pagination element.
        $scope.searchConfig.service = searchConfig.service;
        formatDate($scope.searchConfig.result.data);
      };

      $scope.autoRefreshEnabled = false;
      $scope.onAutoRefreshToggled = function(enabled) {
        $scope.autoRefreshEnabled = enabled;
      };
    }
  ]);
});
