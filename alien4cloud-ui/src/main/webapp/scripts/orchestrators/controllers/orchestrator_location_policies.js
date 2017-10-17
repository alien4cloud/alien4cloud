define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');


  require('scripts/orchestrators/directives/orchestrator_location_policies');
  require('scripts/orchestrators/services/location_resources_security_service');
  require('scripts/users/directives/authorize_users');
  require('scripts/users/directives/authorize_groups');
  require('scripts/users/directives/authorize_apps');

  states.state('admin.orchestrators.details.locations.policies', {
    url: '/policies',
    templateUrl: 'views/orchestrators/orchestrator_location_policies.html',
    controller: 'OrchestratorLocationPoliciesCtrl',
    menu: {
      id: 'menu.orchestrators.locations.policies',
      state: 'admin.orchestrators.details.locations.policies',
      key: 'POLICIES',
      icon: 'fa fa-gavel',
      priority: 300,
      active: true
    }
  });

  modules.get('a4c-orchestrators').controller('OrchestratorLocationPoliciesCtrl', ['$scope',  'locationResourcesSecurity',
    function($scope, locationResourcesSecurity) {

      // populate the scope with the ncessary for location policies resources security
      locationResourcesSecurity('rest/latest/orchestrators/:orchestratorId/locations/:locationId/policies', $scope);

    }
  ]);
}); // define
