define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('admin.orchestrators.details.locations.nodes', {
    url: '/node',
    templateUrl: 'views/orchestrators/orchestrator_locations_nodes.html',
    controller: 'OrchestratorLocationsNodesCtrl',
    menu: {
      id: 'menu.orchestrators.locations.nodes',
      state: 'admin.orchestrators.details.locations.nodes',
      key: 'ORCHESTRATORS.LOCATIONS.ON_DEMAND_RESOURCES',
      icon: 'fa fa-cubes',
      priority: 200
    }
  });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap','a4c-common']).controller('OrchestratorLocationsNodesCtrl',
    ['$scope', 'orchestrator',
    function($scope, orchestrator) {
      $scope.orchestrator = orchestrator;
    }
  ]); // controller
}); // define
