define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var alienUtils = require('scripts/utils/alien_utils');

  states.state('applications.detail.environment.deploycurrent.executiontasks', {
    url: '/execution/:executionId/tasks',
    templateUrl: 'views/_ref/applications/deployment_history/deployment_detail_tasks.html',
    controller: 'ApplicationEnvDeployCurrentTasksCtrl',
    params: {
      execution: null,
      executionId: null
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentTasksCtrl',
    ['$scope', '$state', 'breadcrumbsService', '$translate', 'searchServiceFactory',
    function($scope, $state, breadcrumbsService, $translate, searchServiceFactory) {

      $scope.getTaskStatusIconCss = alienUtils.getTaskStatusIconCss;
      $scope.getTaskStatusTextCss = alienUtils.getTaskStatusTextCss;

      breadcrumbsService.putConfig({
        state : 'applications.detail.environment.deploycurrent.executiontasks',
        text: function() {
          return (_.defined($state.params.execution.workflowName)) ? $state.params.execution.workflowName : $state.params.execution.id;
        }
      });

      $scope.displayLogs = function(task) {
        $state.go('applications.detail.environment.deploycurrent.logs', {
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
