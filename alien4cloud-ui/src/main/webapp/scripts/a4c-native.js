if (typeof define !== 'function') { var define = require('amdefine')(module); }

define(function () {
  'use strict';
  // Return native alien4cloud modules to be packaged.
  return ['scripts/authentication/controllers/navbar',
        'scripts/admin/admin',
        'scripts/applications/controllers/application_list',
        'scripts/users/controllers/users',
        'scripts/orchestrators/controllers/orchestrator_list',
        'scripts/components/controllers/component_list',
        'scripts/topologytemplates/controllers/topology_template_list',
        'scripts/common/directives/empty_place_holder'];
});
