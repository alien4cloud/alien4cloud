define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  var GROUP_ALL = '_A4C_ALL';

  require('scripts/deployment/directives/display_outputs');
  require('scripts/common/filters/inputs');
  require('scripts/applications/services/locations_matching_services');
  require('scripts/applications/services/deployment_topology_services');

  states.state('applications.detail.deployment.locations', {
    url: '/locations',
    templateUrl: 'views/applications/application_deployment_locations.html',
    controller: 'ApplicationDeploymentLocationCtrl',
    menu: {
      id: 'am.applications.detail.deployment.locations',
      state: 'applications.detail.deployment.locations',
      key: 'ORCHESTRATORS.NAV.LOCATIONS',
      icon: 'fa fa-cloud-upload',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentLocationCtrl',
    ['$scope', 'authService', '$upload', 'applicationServices', 'toscaService', 'locationsMatchingServices', '$resource', '$http', '$translate', 'application', '$state', 'applicationEnvironmentServices', 'appEnvironments', 'toaster', '$filter', 'menu', 'deploymentTopologyServices',
      function($scope, authService, $upload, applicationServices, toscaService, locationsMatchingServices, $resource, $http, $translate, applicationResult, $state, applicationEnvironmentServices, appEnvironments, toaster, $filter, menu, deploymentTopologyServices) {

        var nodeMatchingMenuItem;
        _.each(menu, function(menuItem) {
          if (menuItem.id === 'am.applications.detail.deployment.match') {
            nodeMatchingMenuItem = menuItem;
          }
        });

        function formatLocationMatches(locationMatches) {
          $scope.deploymentContext.locationMatches = {};
          _.each(locationMatches, function(locationMatch) {
            $scope.deploymentContext.locationMatches[locationMatch.location.id] = locationMatch;
          });
        }

        var refreshLocationMatching = function() {
          locationsMatchingServices.getLocationsMatches({topologyId: $scope.topologyId}, function(result) {
            formatLocationMatches(result.data);
            initSelectedLocation();
            enableDisableNodeMatchingMenu();
          });
        };

        // Watch over deployment topology to initialize selected location
        $scope.$watch('deploymentContext.deploymentTopologyDTO', function() {
          refreshLocationMatching();
        });

        //check and fill selected location from deploymentTopologyDTO
        function initSelectedLocation() {
          delete $scope.deploymentContext.selectedLocation;
          if (_.has($scope, 'deploymentContext.deploymentTopologyDTO.locationPolicies.' + GROUP_ALL)) {
            var selectedLocationId = $scope.deploymentContext.deploymentTopologyDTO.locationPolicies[GROUP_ALL];
            if ($scope.deploymentContext.locationMatches && $scope.deploymentContext.locationMatches[selectedLocationId]) {
              $scope.deploymentContext.selectedLocation = $scope.deploymentContext.locationMatches[selectedLocationId].location;
            }
          }
        }

        //enable or not the node matching menu
        function enableDisableNodeMatchingMenu() {
          nodeMatchingMenuItem.disabled = _.undefined($scope.deploymentContext.selectedLocation);
        }

        $scope.selectLocation = function(locationMatch) {
          var groupsToLocations = {};
          groupsToLocations[GROUP_ALL] = locationMatch.location.id;

          var configRequest = {
            orchestratorId: locationMatch.location.orchestratorId,
            groupsToLocations: groupsToLocations
          };

          deploymentTopologyServices.setLocationPolicies({
            appId: $scope.application.id,
            envId: $scope.deploymentContext.selectedEnvironment.id
          }, angular.toJson(configRequest), function(result) {
//          console.log('Result of set location is: ',result);
            $scope.deploymentContext.selectedLocation = locationMatch.location;
            enableDisableNodeMatchingMenu();
//          console.debug('Selected Location is: ', locationMatch.location.name)
            $state.go('applications.detail.deployment.match');
          });
        };
        // checks if a location is the selected one for this deployment
        $scope.isLocationSelected = function(location) {
          return _.has($scope, "deploymentContext.selectedLocation") && $scope.deploymentContext.selectedLocation.id === location.id;
        };
      }
    ]); //controller
}); //Define
