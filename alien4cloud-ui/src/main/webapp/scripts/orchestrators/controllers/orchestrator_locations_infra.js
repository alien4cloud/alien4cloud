define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('admin.orchestrators.details.locations.infra', {
    url: '/infra',
    templateUrl: 'views/orchestrators/orchestrator_locations_infra.html',
    controller: 'OrchestratorLocationsConfigCtrl',
    menu: {
      id: 'menu.orchestrators.locations.infra',
      state: 'admin.orchestrators.details.locations.infra',
      key: 'ORCHESTRATORS.LOCATIONS.CONFIGURATION_RESOURCES',
      icon: 'fa fa-wrench',
      priority: 100
    }
  });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('OrchestratorLocationsConfigCtrl',
    ['$scope', 'orchestrator',
      function($scope, orchestrator) {
        $scope.orchestrator = orchestrator;
        if (_.isNotEmpty($scope.context.configurationTypes)) {
          $scope.selectedConfigurationResource = $scope.context.configurationTypes[0];
        }
      }
    ]); // controller
}); // define
