define(function (require) {
  'use strict';

  var modules = require('modules');
  var alienUtils = require('scripts/utils/alien_utils');

  modules.get('a4c-applications').controller('DeploymentExecutionDetailInfoCtrl',
    ['$scope', '$state', 'searchServiceFactory',
      function ($scope, $state, searchServiceFactory) {
        console.log("ENtering task scope");

//        $scope.deploymentDTO = deploymentDTO.data;
//        $scope.executionId = executionId;
//        $scope.executionStatusIconCss = alienUtils.getExecutionStatusIconCss;
//        $scope.executionStatusTextCss = alienUtils.getExecutionStatusTextCss;
//
//        $scope.displayLogs = function(taskId) {
//          $state.go('applications.detail.environment.history.detail.logs', {
//            'deploymentId': deploymentDTO.data.deployment.id,
//            'executionId': execution.id,
//            'taskId': taskId
//          });
//        };
//
//        $scope.now = new Date();
//
//        var searchServiceUrl = 'rest/latest/tasks/search';
//        $scope.queryManager = {
//          query: ''
//        };
//        $scope.searchService = searchServiceFactory(searchServiceUrl, true, $scope.queryManager, 30, 50, true, null, { executionId: execution.id });
//        $scope.searchService.search();
//        $scope.queryManager.onSearchCompleted = function(searchResult) {
//          $scope.tasks = searchResult.data.data;
//        };
      }
    ]);
});
