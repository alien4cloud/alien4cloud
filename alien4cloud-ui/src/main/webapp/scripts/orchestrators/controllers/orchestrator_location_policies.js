define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  // states.state('admin.orchestrators.details.locations.policies', {
  //   url: '/policy',
  //   templateUrl: 'views/orchestrators/orchestrator_location_policies.html',
  //   controller: 'OrchestratorLocationsPoliciesCtrl',
  //   menu: {
  //     id: 'menu.orchestrators.locations.policies',
  //     state: 'admin.orchestrators.details.locations.policies',
  //     key: 'ORCHESTRATORS.LOCATIONS.POLICIES',
  //     icon: 'fa fa-cogs',
  //     priority: 400
  //   }
  // });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap','a4c-common']).controller('OrchestratorLocationsPoliciesCtrl',
    ['$scope', 'orchestrator',
    function($scope, orchestrator) {
      $scope.orchestrator = orchestrator;
    }
  ]); // controller
}); // define
