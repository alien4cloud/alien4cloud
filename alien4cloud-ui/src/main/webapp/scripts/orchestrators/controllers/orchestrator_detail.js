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

  states.state('admin.orchestrators.detail', {
    url: '/detail/:id',
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
    templateUrl: 'views/orchestrators/orchestrator_detail_layout.html',
    controller: 'LayoutCtrl'
  });

  states.state('admin.orchestrators.detail.info', {
    url: '/info',
    templateUrl: 'views/orchestrators/orchestrator_info.html',
    controller: 'OrchestratorArtifactsCtrl',
    menu: {
      id: 'menu.orchestrators.info',
      state: 'admin.orchestrators.detail.info',
      key: 'NAVBAR.MENU_APPS',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  states.forward('admin.orchestrators.detail', 'admin.orchestrators.detail.info');

  modules.get('a4c-orchestrators').controller('OrchestratorArtifactsCtrl',
    ['$scope', '$modal', '$state',
    function($scope, $modal, $state) {
    }
  ]); // controller
}); // define
