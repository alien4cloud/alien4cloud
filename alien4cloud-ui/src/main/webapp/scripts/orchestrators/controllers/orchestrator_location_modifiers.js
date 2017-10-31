define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  states.state('admin.orchestrators.details.locations.modifiers', {
    url: '/modifiers',
    templateUrl: 'views/orchestrators/orchestrator_location_modifiers.html',
    controller: 'OrchestratorLocationModifiersCtrl',
    menu: {
      id: 'menu.orchestrators.locations.modifiers',
      state: 'admin.orchestrators.details.locations.modifiers',
      key: 'TOPOLOGY.MODIFIERS',
      icon: 'fa fa-random',
      priority: 400,
      active: true
    }
  });

  modules.get('a4c-orchestrators').controller('OrchestratorLocationModifiersCtrl', ['$scope',
    function($scope) {

    }
  ]);
}); // define
