define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/controllers/orchestrator_locations_config');
  require('scripts/orchestrators/controllers/orchestrator_locations_nodes');
  require('scripts/orchestrators/controllers/orchestrator_locations_policies');
  require('scripts/orchestrators/controllers/orchestrator_locations_services');

  states.state('admin.orchestrators.details.locations', {
    url: '/locations',
    templateUrl: 'views/orchestrators/orchestrator_locations.html',
    controller: 'OrchestratorLocationsCtrl',
    menu: {
      id: 'menu.orchestrators.locations',
      state: 'admin.orchestrators.details.locations',
      key: 'ORCHESTRATORS.NAV.LOCATIONS',
      icon: 'fa fa-cloud',
      priority: 400
    }
  });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap','a4c-common']).controller('OrchestratorLocationsCtrl',
    ['$scope', 'orchestrator', 'menu',
    function($scope, orchestrator, menu) {
      $scope.orchestrator = orchestrator;
      $scope.menu = menu;
    }
  ]); // controller
}); // define
