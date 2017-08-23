define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('applications.detail.environment.deploynext.locations', {
    url: '/locations',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_locations.html',
    controller: 'AppEnvDeployNextLocationsCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.locations',
      state: 'applications.detail.environment.deploynext.locations',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT.LOCATIONS',
      icon: '',
      priority: 400,
      step: {
        // task code in validation DTO bound to this step
        taskCodes: ['LOCATION_POLICY', 'LOCATION_DISABLED', 'LOCATION_UNAUTHORIZED', 'CFY_MULTI_RELATIONS']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextLocationsCtrl',
    ['$scope', 'deploymentTopologyServices', 'locationsMatchingServices',
    function ($scope, deploymentTopologyServices, locationsMatchingServices) {

      if (_.has($scope, 'deploymentContext.deploymentTopologyDTO.topology.orchestratorId') && _.has($scope, 'deploymentContext.deploymentTopologyDTO.locationPolicies.' + GROUP_ALL)) {
        $scope.oldSelectedOrchestratorId = $scope.deploymentContext.deploymentTopologyDTO.topology.orchestratorId;
        $scope.oldSelectedLocationId = $scope.deploymentContext.deploymentTopologyDTO.locationPolicies[GROUP_ALL];
      }

      //select a location
      $scope.selectLocation = function(locationMatch) {
        // Do nothing if already selected or not ready
        if(locationMatch.selected || !locationMatch.ready){
          return;
        }

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
          $scope.goToNextInvalidStep();
          // $state.go(thisMenu.nextStep.state);
        });
      };
    }
  ]);
});
