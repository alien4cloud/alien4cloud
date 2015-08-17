define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('admin.orchestrators.detail.locations', {
    url: '/locations',
    templateUrl: 'views/orchestrators/orchestrator_locations.html',
    controller: 'OrchestratorLocationsCtrl',
    menu: {
      id: 'menu.orchestrators.locations',
      state: 'admin.orchestrators.detail.locations',
      key: 'NAVBAR.MENU_APPS',
      icon: 'fa fa-cloud',
      priority: 400
    }
  });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap','a4c-common']).controller('OrchestratorLocationsCtrl',
    ['$scope', 'orchestrator',
    function($scope, orchestrator) {
      $scope.orchestrator = orchestrator;
      
    }
  ]); // controller
}); // define
