define(function (require) {
  'use strict';
  
  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');
  require('scripts/users/directives/users_authorization');
  require('scripts/users/directives/groups_authorization');
  require('scripts/users/directives/apps_authorization');
  require('scripts/orchestrators/services/location_security_service');
  
  states.state('admin.orchestrators.details.locations.security', {
    url: '/security',
    templateUrl: 'views/orchestrators/orchestrator_location_security.html',
    menu: {
      id: 'menu.orchestrators.locations.security',
      state: 'admin.orchestrators.details.locations.security',
      key: 'ORCHESTRATORS.LOCATIONS.SECURITY',
      icon: 'fa fa-users',
      priority: 600
    }
  });
}); // define
