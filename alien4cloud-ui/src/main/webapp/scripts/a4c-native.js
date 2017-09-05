if (typeof define !== 'function') { var define = require('amdefine')(module); }

define(function () {
  'use strict';
  // Return native alien4cloud modules to be packaged.
  return ['scripts/authentication/controllers/navbar',
        'scripts/_ref/admin/controllers/admin',
        'scripts/common/controllers/maintenance',
        'scripts/_ref/applications/controllers/applications_list',
        'scripts/users/controllers/users',
        'scripts/orchestrators/controllers/orchestrator_list',
        'scripts/services/controllers/service_resources',
        'scripts/_ref/catalog/controllers/catalog',
        'scripts/common/directives/empty_place_holder',
        'scripts/common/directives/resizable_bar'];
});
