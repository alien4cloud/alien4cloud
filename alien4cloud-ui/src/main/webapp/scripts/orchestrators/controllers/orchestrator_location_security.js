define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('admin.orchestrators.details.locations.details.security', {
    url: '/infra',
    templateUrl: 'views/orchestrators/orchestrator_locations_security.html',
    controller: 'OrchestratorLocationsConfigCtrl',
    menu: {
      id: 'menu.orchestrators.locations.details.security',
      state: 'admin.orchestrators.details.locations.details.security',
      key: 'NAVBAR.MENU_APPS',
      icon: 'fa fa-users',
      priority: 200
    }
  });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap','a4c-common']).controller('OrchestratorLocationsConfigCtrl',
    ['$scope', 'orchestrator',
    function($scope, orchestrator) {
      $scope.orchestrator = orchestrator;
    }
  ]); // controller
}); // define
