define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/common/services/global_rest_error_handler');

  require('scripts/orchestrators/services/orchestrator_service');
  require('scripts/orchestrators/services/orchestrator_properties_service');
  require('scripts/orchestrators/services/orchestrator_instance_service');
  require('scripts/orchestrators/controllers/orchestrator_artifacts');
  require('scripts/orchestrators/controllers/orchestrator_configuration');
  require('scripts/orchestrators/controllers/orchestrator_locations');
  require('scripts/orchestrators/controllers/orchestrator_deployments');
  require('scripts/orchestrators/controllers/orchestrator_security');

  states.state('admin.orchestrators.details', {
    url: '/details/:id',
    resolve: {
      orchestrator: ['orchestratorService', '$stateParams',
        function(orchestratorService, $stateParams) {
          return orchestratorService.get({orchestratorId: $stateParams.id}).$promise.then(function(result){ return result.data; });
        }
      ],
      context: [ 'orchestrator',
        function(orchestrator) {
          return {orchestrator: orchestrator};
        }
      ]
    },
    templateUrl: 'views/orchestrators/orchestrator_details_layout.html',
    controller: 'LayoutCtrl'
  });

  states.state('admin.orchestrators.details.info', {
    url: '/info',
    templateUrl: 'views/orchestrators/orchestrator_info.html',
    controller: 'OrchestratorDetailsCtrl',
    menu: {
      id: 'menu.orchestrators.info',
      state: 'admin.orchestrators.details.info',
      key: 'ORCHESTRATORS.NAV.INFO',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  states.forward('admin.orchestrators.details', 'admin.orchestrators.details.info');

  modules.get('a4c-orchestrators').controller('OrchestratorDetailsCtrl',
    ['$scope', '$modal', '$state', '$translate', 'orchestratorService', 'orchestratorInstanceService', 'orchestrator', 'metapropConfServices', 'globalRestErrorHandler',
    function($scope, $modal, $state, $translate, orchestratorService, orchestratorInstanceService, orchestrator, metapropConfServices, globalRestErrorHandler) {
      $scope.orchestrator = orchestrator;
      $scope.showForceDisable = false;

      $scope.updateName = function(name) {
        if (name !== orchestrator.name) {
          var request = {name:name};
          return orchestratorService.update({orchestratorId: orchestrator.id}, angular.toJson(request)).$promise.then(
            function() {}, // Success
            function(errorResponse) {
              return $translate.instant('ERRORS.' + errorResponse.data.error.code);
            });
        }
      };

      $scope.removeOrchestrator = function() {
        orchestratorService.remove({orchestratorId: orchestrator.id}).$promise.then(
          function(result) {
            if (!result.error) {
              $state.go('admin.orchestrators.list');
            }
          });
      };

      $scope.enable = function() {
        orchestrator.state = 'CONNECTING';
        orchestratorInstanceService.create({orchestratorId: orchestrator.id}, {},function(){})//do something here
          .$promise['finally'](function() {
            $state.reload(); // do something with web-sockets to get notifications on the orchestrator state.
          });
      };

      $scope.disable = function(force) {
        orchestrator.state = 'DISABLING';
        orchestratorInstanceService.remove({orchestratorId: orchestrator.id, force: force})
          .$promise.then(function(result){
            globalRestErrorHandler.handle(result);
          })
          ['finally'](function() {
            $state.reload(); // do something with web-sockets to get notifications on the orchestrator state.
          });
      };

      $scope.loadConfigurationTag = function() {
        // filter only by target 'orchestrator'
        var filterOrchestrator = {};
        filterOrchestrator.target = [];
        filterOrchestrator.target.push('orchestrator');

        var searchRequestObject = {
          'query': '',
          'filters': filterOrchestrator,
          'from': 0,
          'size': 50
        };

        metapropConfServices.search([], angular.toJson(searchRequestObject), function(successResult) {
          $scope.orchestratorProperties = successResult.data.data;
        });
      };
      $scope.loadConfigurationTag();
    }
  ]); // controller
}); // define
