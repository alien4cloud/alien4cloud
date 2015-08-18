define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('admin.orchestrators.details.locations.services', {
    url: '/infra',
    templateUrl: 'views/orchestrators/orchestrator_locations_services.html',
    controller: 'OrchestratorLocationsServicesCtrl',
    menu: {
      id: 'menu.orchestrators.locations.services',
      state: 'admin.orchestrators.details.locations.services',
      key: 'ORCHESTRATORS.LOCATIONS.SERVICES',
      icon: 'fa fa-retweet',
      priority: 300
    }
  });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap','a4c-common']).controller('OrchestratorLocationsServicesCtrl',
    ['$scope', 'orchestrator',
    function($scope, orchestrator) {
      $scope.orchestrator = orchestrator;
    }
  ]); // controller
}); // define
