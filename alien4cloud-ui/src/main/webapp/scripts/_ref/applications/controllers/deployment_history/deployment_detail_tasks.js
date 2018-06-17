define(function (require) {
  'use strict';

  var modules = require('modules');
  var alienUtils = require('scripts/utils/alien_utils');

  modules.get('a4c-applications').controller('DeploymentExecutionDetailInfoCtrl',
    ['$scope', '$state', 'searchServiceFactory',
      function ($scope, $state, searchServiceFactory) {
        $scope.getTaskStatusIconCss = alienUtils.getTaskStatusIconCss;
        $scope.getTaskStatusTextCss = alienUtils.getTaskStatusTextCss;

        $scope.displayLogs = function(task) {
          $state.go('applications.detail.environment.history.detail.logs', {
            'applicationId': $scope.application.id,
            'applicationEnvironmentId': $scope.environment.id,
            'executionId': $state.params.execution.id,
            'taskId': task.id
          });
        };

        var searchServiceUrl = 'rest/latest/tasks/search';
        $scope.queryManager = {
          query: ''
        };
        $scope.searchService = searchServiceFactory(searchServiceUrl, true, $scope.queryManager, 15, 50, true, null, { executionId: $state.params.execution.id });
        $scope.searchService.search();
        $scope.queryManager.onSearchCompleted = function(searchResult) {
          $scope.stepTasks = searchResult.data.data;
        };

        $scope.$on('a4cRuntimeEventReceived', function(angularEvent, event) {
            if(event.rawType === 'paasworkflowmonitorevent') {
                $scope.searchService.search();
            }
        });
      }
    ]);
});
