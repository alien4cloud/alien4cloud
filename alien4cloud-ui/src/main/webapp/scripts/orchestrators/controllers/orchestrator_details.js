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
    templateUrl: 'views/_ref/layout/vertical_menu_left_layout.html',
    controller: 'OrchestratorDetailsCtrl'
  });

  states.state('admin.orchestrators.details.info', {
    url: '/info',
    templateUrl: 'views/orchestrators/orchestrator_info.html',
    controller: 'OrchestratorDetailsInfoCtrl',
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
  ['$scope', '$state', '$translate', 'orchestrator', 'breadcrumbsService', 'menu', 'layoutService', 'context',
  function($scope, $state, $translate, orchestrator, breadcrumbsService, menu, layoutService, context) {
      breadcrumbsService.putConfig({
        state: 'admin.orchestrators.details',
        text: function() {
          return orchestrator.name;
        },
        onClick: function() {
          $state.go('admin.orchestrators.details', { id: orchestrator.id });
        }
      });

      $scope.context = context;
      layoutService.process(menu);
      $scope.menu = menu;
    }
  ]);


  modules.get('a4c-orchestrators').controller('OrchestratorDetailsInfoCtrl',
    ['$scope', '$state', '$translate', 'orchestratorService', 'orchestratorInstanceService', 'orchestrator', 'metapropConfServices', 'globalRestErrorHandler', 'breadcrumbsService',
    function($scope, $state, $translate, orchestratorService, orchestratorInstanceService, orchestrator, metapropConfServices, globalRestErrorHandler, breadcrumbsService) {

      breadcrumbsService.putConfig({
        state: 'admin.orchestrators.details.info',
        text: function() {
          return $translate.instant('ORCHESTRATORS.NAV.INFO');
        }
      });

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
        orchestratorInstanceService.create({orchestratorId: orchestrator.id}, {}).$promise
        .then(result => {
          if(result.error){
            globalRestErrorHandler.handle(result);
            $scope.disable(false);
          }
        })
        .then(anyway => {
          $state.reload();
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
