define(function (require) {
  'use strict';

  var modules = require('modules');
  var alienUtils = require('scripts/utils/alien_utils');

  modules.get('a4c-applications').controller('DeploymentDetailInfoCtrl',
    ['$scope', '$state', 'deploymentDTO', 'searchServiceFactory',
      function ($scope, $state, deploymentDTO, searchServiceFactory) {

        $scope.deploymentDTO = deploymentDTO.data;
        $scope.executionStatusIconCss = alienUtils.getExecutionStatusIconCss;
        $scope.executionStatusTextCss = alienUtils.getExecutionStatusTextCss;

        $scope.displayLogs = function(executionId) {
          var logURL = 'applications.detail.environment.history.detail.logs';
            if ($state.href(logURL)) {
                $state.go(logURL, {
                  'deploymentDTO': deploymentDTO.data,
                  'deploymentId': deploymentDTO.data.deployment.id,
                  'environmentId': deploymentDTO.data.deployment.environmentId,
                  'id': deploymentDTO.data.source.id,
                  'executionId': executionId
                });
            } else {
              // TODO: Show an error message that the log plugin has not been uploaded.
              console.log("The log plugin has not been uploaded.")
            }

        };

        $scope.displayTasks = function(execution) {
          // Attention: should pass all the param needed for the next state
          // which is not always being the children of current state
          $state.go('applications.detail.environment.history.detail.tasks', {
            'deploymentDTO': deploymentDTO.data,
            'deploymentId': deploymentDTO.data.deployment.id,
            'environmentId': deploymentDTO.data.deployment.environmentId,
            'id': deploymentDTO.data.source.id,
            'execution': execution,
            'executionId': execution.id
          });
        };

        $scope.now = new Date();

        var searchServiceUrl = 'rest/latest/executions/search';
        $scope.queryManager = {
          query: ''
        };
        $scope.searchService = searchServiceFactory(searchServiceUrl, true, $scope.queryManager, 15, 50, true, null, { deploymentId: deploymentDTO.data.deployment.id });
        $scope.searchService.search();
        $scope.queryManager.onSearchCompleted = function(searchResult) {
          $scope.executions = searchResult.data.data;
        };
      }
    ]);
});
