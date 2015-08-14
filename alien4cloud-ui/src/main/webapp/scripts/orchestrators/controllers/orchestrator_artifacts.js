define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('admin.orchestrators.details.artifacts', {
    url: '/artifacts',
    templateUrl: 'views/orchestrators/orchestrator_artifacts.html',
    controller: 'OrchestratorArtifactsCtrl',
    menu: {
      id: 'menu.orchestrators.artifacts',
      state: 'admin.orchestrators.details.artifacts',
      key: 'NAVBAR.MENU_APPS',
      icon: 'fa fa-file-text-o',
      priority: 500
    }
  });

  modules.get('a4c-orchestrators').controller('OrchestratorArtifactsCtrl',
    ['$scope', '$modal', '$state',
    function($scope, $modal, $state) {
    }
  ]); // controller
}); // define
