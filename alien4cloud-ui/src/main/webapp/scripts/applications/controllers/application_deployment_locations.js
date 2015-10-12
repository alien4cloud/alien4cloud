define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');
  var SUBSTITUTION_ID = 'am.applications.detail.deployment.match';
  var LOCATION_ID = 'am.applications.detail.deployment.locations';
  var LOCATION_STEP_CODE = 'LOCATION_POLICY'

  var GROUP_ALL = '_A4C_ALL';

  require('scripts/deployment/directives/display_outputs');
  require('scripts/common/filters/inputs');
  require('scripts/applications/services/locations_matching_services');
  require('scripts/applications/services/deployment_topology_services');
  require('scripts/applications/services/deployment_topology_processor.js');
  require('scripts/applications/services/tasks_processor.js');

  states.state('applications.detail.deployment.locations', {
    url: '/locations',
    templateUrl: 'views/applications/application_deployment_locations.html',
    controller: 'ApplicationDeploymentLocationCtrl',
    resolve: {
      thisStepMenu: ['menu', function(menu){
        return _.find(menu, function(item){
          return item.id==='am.applications.detail.deployment.locations'
        })
      }],
      nextStepMenu: ['menu', function(menu){
        return _.find(menu, function(item){
          return item.id==='am.applications.detail.deployment.match'
        });
      }]
    },
    menu: {
      id: 'am.applications.detail.deployment.locations',
      state: 'applications.detail.deployment.locations',
      key: 'ORCHESTRATORS.NAV.LOCATIONS',
      icon: 'fa fa-cloud-upload',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer
      priority: 100,
      nextStepId: 'am.applications.detail.deployment.match',
      step: {
        // task code in validation DTO bound to this step
        taskCodes: [LOCATION_STEP_CODE]
      }
    }
  });
  
  modules.get('a4c-applications').controller('ApplicationDeploymentLocationCtrl',
    ['$scope', 'locationsMatchingServices', '$state', 'menu', 'deploymentTopologyServices', 'deploymentTopologyProcessor', 'tasksProcessor','thisStepMenu','nextStepMenu',
      function($scope, locationsMatchingServices, $state, menu, deploymentTopologyServices, deploymentTopologyProcessor, tasksProcessor, thisStepMenu, nextStepMenu) {

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
          });
        };
        
        // Watch over deployment topology to initialize selected location
        $scope.$watch('deploymentContext.deploymentTopologyDTO', function() {
//          console.log($scope.deploymentContext.deploymentTopologyDTO)
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
          }, angular.toJson(configRequest), function(response) {
            $scope.updateScopeDeploymentTopologyDTO(response.data);
            $scope.deploymentContext.selectedLocation = locationMatch.location;
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
