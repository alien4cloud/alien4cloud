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
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT_LOCATIONS',
      icon: '',
      priority: 400,
      step: {
        // task code in validation DTO bound to this step
        taskCodes: ['LOCATION_POLICY', 'LOCATION_DISABLED', 'LOCATION_UNAUTHORIZED', 'CFY_MULTI_RELATIONS']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextLocationsCtrl',
    ['$scope', 'deploymentTopologyServices', 'locationsMatchingServices', 'breadcrumbsService', '$translate', '$state',
    function ($scope, deploymentTopologyServices, locationsMatchingServices, breadcrumbsService, $translate, $state) {

      breadcrumbsService.putConfig({
        state: 'applications.detail.environment.deploynext.locations',
        text: function () {
          return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_NEXT_LOCATIONS');
        },
        onClick: function () {
          $state.go('applications.detail.environment.deploynext.locations');
        }
      });
      $scope.$watch('locationMatches', function(newValue){
        $scope.orchestrators = {};
        if( _.defined(newValue) ) {
          _.each($scope.locationMatches, function(value){
            $scope.orchestrators[value.orchestrator.id] = value.orchestrator.name
          });
        }
      });

      //select a location
      $scope.selectLocation = function(locationMatch) {

        var groupsToLocations = {};
        var selectedOrchestratorId = "";

        // let retrieve already selected locations if any
        _.each($scope.deploymentTopologyDTO.topology.locationGroups, function(locationGrp) {
          var locationId = locationGrp.policies[0].locationId;
          groupsToLocations[locationId] = locationId;
          selectedOrchestratorId = $scope.locationMatches[locationId].orchestrator.id;
        });

        if (locationMatch.selected) {
          // Remove selected location from group
          delete groupsToLocations[locationMatch.location.id];
        } else {
          if (locationMatch.location.orchestratorId != selectedOrchestratorId) {
            // Selected a location from another orchestrator so let reset the map
            groupsToLocations = {};
          }
          groupsToLocations[locationMatch.location.id] = locationMatch.location.id;
        }


        var configRequest = {
          orchestratorId: locationMatch.location.orchestratorId,
          groupsToLocations: groupsToLocations
        };

        deploymentTopologyServices.setLocationPolicies({
          appId: $scope.application.id,
          envId: $scope.environment.id
        }, angular.toJson(configRequest), function(response) {
          $scope.updateScopeDeploymentTopologyDTO(response.data);
          $scope.goToNextInvalidStep();
          // $state.go(thisMenu.nextStep.state);
        });
      };
    }
  ]);
});
