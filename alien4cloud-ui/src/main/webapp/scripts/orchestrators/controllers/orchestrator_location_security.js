define(function (require) {
  'use strict';
  
  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');
  
  require('scripts/orchestrators/services/location_security_service');
  
  states.state('admin.orchestrators.details.locations.security', {
    url: '/security',
    templateUrl: 'views/orchestrators/orchestrator_location_security.html',
    controller: 'OrchestratorLocationsSecurityCtrl',
    menu: {
      id: 'menu.orchestrators.locations.security',
      state: 'admin.orchestrators.details.locations.security',
      key: 'ORCHESTRATORS.LOCATIONS.SECURITY',
      icon: 'fa fa-users',
      priority: 1
    }
  });
  
  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('OrchestratorLocationsSecurityCtrl',
    ['$scope', 'orchestrator', '$resource', 'userServices', 'groupServices', 'locationSecurityService',
      function ($scope, orchestrator, $resource, userServices, groupServices, locationSecurityService) {
      }
    ]); // controller
}); // define
