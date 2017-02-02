define(function (require) {
  'use strict';
  var modules = require('modules');
  var states = require('states');
  require('scripts/users/directives/users_authorization');
  require('scripts/users/directives/groups_authorization');
  require('scripts/users/directives/apps_authorization');
  require('scripts/common/services/resource_security_factory');

  states.state('admin.orchestrators.details.locations.security', {
    url: '/security',
    templateUrl: 'views/orchestrators/orchestrator_location_security.html',
    controller: 'OrchestratorLocationSecurityCtrl',
    menu: {
      id: 'menu.orchestrators.locations.security',
      state: 'admin.orchestrators.details.locations.security',
      key: 'ORCHESTRATORS.LOCATIONS.SECURITY',
      icon: 'fa fa-users',
      priority: 600
    }
  });

  modules.get('a4c-orchestrators').controller('OrchestratorLocationSecurityCtrl', ['$scope', 'resourceSecurityFactory',
    function($scope, resourceSecurityFactory) {

      // NOTE: locationId is a function, so that it will be evaluated everytime a REST call will be made
      // this is because the selected location can change within the page
      var locationSecurityService = resourceSecurityFactory('rest/latest/orchestrators/:orchestratorId/locations/:locationId', {
        orchestratorId: $scope.orchestrator.id,
        locationId: function () {
          return $scope.context.location.id;
        }
      });
      $scope.locationSecurityService = locationSecurityService;
    }
  ]);

}); // define
