define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/services/orchestrator_service');
  require('scripts/orchestrators/controllers/orchestrator_artifacts');
  require('scripts/orchestrators/controllers/orchestrator_configuration');
  require('scripts/orchestrators/controllers/orchestrator_location');
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
    controller: 'OrchestratorArtifactsCtrl',
    menu: {
      id: 'menu.orchestrators.info',
      state: 'admin.orchestrators.details.info',
      key: 'NAVBAR.MENU_APPS',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  states.forward('admin.orchestrators.details', 'admin.orchestrators.details.info');

  modules.get('a4c-orchestrators').controller('OrchestratorArtifactsCtrl',
    ['$scope', '$modal', '$state', 'orchestratorService', 'orchestrator',
    function($scope, $modal, $state, orchestratorService, orchestrator) {
      $scope.updateOrchestrator = function(name){
        if (name !== orchestrator.name) {
          orchestratorService.update({orchestratorId: orchestrator.id}, name).$promise.then(function(result){ return result.data; });
        }
      }
    }
  ]); // controller
}); // define
