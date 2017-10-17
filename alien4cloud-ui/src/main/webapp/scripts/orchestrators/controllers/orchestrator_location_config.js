define(function(require) {
  'use strict';

  var states = require('states');

  require('scripts/orchestrators/directives/orchestrator_location_resources');

  states.state('admin.orchestrators.details.locations.config', {
    url: '/config',
    templateUrl: 'views/orchestrators/orchestrator_location_config.html',
    menu: {
      id: 'menu.orchestrators.locations.config',
      state: 'admin.orchestrators.details.locations.config',
      key: 'ORCHESTRATORS.LOCATIONS.CONFIGURATION_RESOURCES',
      icon: 'fa fa-wrench',
      priority: 100
    }
  });
}); // define
