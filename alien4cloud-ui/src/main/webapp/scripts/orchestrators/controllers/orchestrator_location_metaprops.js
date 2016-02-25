define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/meta-props/directives/meta_props_display');

  states.state('admin.orchestrators.details.locations.metaprops', {
    url: '/metaprops',
    templateUrl: 'views/orchestrators/orchestrator_location_metaprops.html',
    controller: 'OrchestratorLocationsMetaPropsCtrl',
    menu: {
      id: 'menu.orchestrators.locations.policies',
      state: 'admin.orchestrators.details.locations.metaprops',
      key: 'NAVADMIN.MENU_TAGS',
      icon: 'fa fa-tags',
      priority: 500
    }
  });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap','a4c-common']).controller('OrchestratorLocationsMetaPropsCtrl',
    ['$scope', 'orchestrator',
    function($scope, orchestrator) {
      $scope.orchestrator = orchestrator;
    }
  ]); // controller
}); // define
