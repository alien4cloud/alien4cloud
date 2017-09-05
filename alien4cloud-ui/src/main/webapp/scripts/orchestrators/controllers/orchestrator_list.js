define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/common/services/alien_resource');
  require('scripts/orchestrators/controllers/orchestrator_new');
  require('scripts/orchestrators/services/orchestrator_service');
  require('scripts/orchestrators/controllers/orchestrator_details');
  require('scripts/orchestrators/controllers/orchestrator');
  
  states.state('admin.orchestrators.list', {
    url: '/list',
    templateUrl: 'views/orchestrators/orchestrator_list.html',
    controller: 'OrchestratorListCtrl'
  });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap','a4c-common']).controller('OrchestratorListCtrl',
    ['$scope', 'searchServiceFactory', '$uibModal', '$state', 'orchestratorService',
    function($scope, searchServiceFactory, $uibModal, $state, orchestratorService) {
      $scope.query = '';
      // onSearchCompleted is used as a callaback for the searchServiceFactory and triggered when the search operation is completed.
      $scope.onSearchCompleted = function(searchResult) {
        $scope.orchestrators = searchResult.data.data;
      };
      // we have to insert the search service in the scope so it is available for the pagination directive.
      $scope.searchService = searchServiceFactory('rest/latest/orchestrators', true, $scope, 20);
      $scope.search = function() {$scope.searchService.search();};
      $scope.search(); // initialize

      $scope.openOrchestrator = function(orchestratorId) {
        $state.go('admin.orchestrators.details', { id: orchestratorId });
      };

      $scope.openNewModal = function() {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/orchestrators/orchestrator_new.html',
          controller: 'NewOrchestratorController'
        });

        modalInstance.result.then(function(newOrchestrator) {
          var orchestrator = {
            name: newOrchestrator.name,
            pluginId: newOrchestrator.plugin.pluginId,
            pluginBean: newOrchestrator.plugin.componentDescriptor.beanName
          };
          orchestratorService.create([], angular.toJson(orchestrator), function() {
            $scope.searchService.search();
          });
        });
      };
    }
  ]); // controller
}); // define
