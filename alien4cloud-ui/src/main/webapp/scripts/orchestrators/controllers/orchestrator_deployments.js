define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('admin.orchestrators.details.deployments', {
    url: '/deployments',
    templateUrl: 'views/orchestrators/orchestrator_deployments.html',
    controller: 'OrchestratorDeploymentsCtrl',
    menu: {
      id: 'menu.orchestrators.deployments',
      state: 'admin.orchestrators.details.deployments',
      key: 'ORCHESTRATORS.NAV.DEPLOYMENTS',
      icon: 'fa fa-rocket',
      priority: 200
    }
  });

  modules.get('a4c-orchestrators').controller('OrchestratorDeploymentsCtrl',
    ['$scope', '$modal', '$state',
    function($scope, $modal, $state) {
    }
  ]); // controller
}); // define
