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

      $scope.$watch('deploymentTopologyDTO', function(newValue){
        $scope.locationGroups = _.cloneDeep(newValue.topology.locationGroups);
      })

      $scope.selectedGroups = {};

      $scope.groupClick = function(group) {
        if (_.has($scope.selectedGroups, group.name)) {
          // was selected, deselect it
          delete $scope.selectedGroups[group.name];
        } else {
          $scope.selectedGroups[group.name] = group;
        }

        // Now display orchestrator & location or selected groups if they are the same
        var commonOrchestrator= "";
        var commonLocation = "";
        _.each($scope.selectedGroups, function(group) {
          if (group.policies) {
            var grpLoc = group.policies[0].locationId;
            var grpOrc = $scope.locationMatches[grpLoc].orchestrator.id;
            if (commonLocation == "") {
              commonLocation = grpLoc;
            } else if (commonLocation != grpLoc) {
              commonLocation = null;
            }
            if (commonOrchestrator == "") {
              commonOrchestrator = grpOrc;
            } else if (commonOrchestrator != grpOrc) {
              commonOrchestrator = null;
              commonLocation = null;
            }
          }
        });

        //if (commonOrchestrator != null && commonOrchestrator != "") {
          $scope.selectedOrchestratorId=commonOrchestrator;
        //}

        //if (commonLocation != null && commonLocation != "") {
          $scope.selectedLocationMatch=commonLocation;
        //}

        $scope.hasGroupSelected = !_.isEmpty($scope.selectedGroups);
      }

      $scope.hasGroupSelected = false;

      $scope.groupsToLocations = {};

      $scope.isGroupSelected = function(locationGroupName) {
        return _.has($scope.selectedGroups, locationGroupName);
      }

      $scope.isGroupReady = function(locationGroup) {
        return !_.has($scope.selectedGroups, locationGroup.name) && locationGroup.policies;
      }

      $scope.isGroupDanger = function(locationGroup) {
        return !_.has($scope.selectedGroups, locationGroup.name) && !locationGroup.policies;
      }

      //select a location
      $scope.selectLocation = function(locationMatchId) {

        if (_.isNull(locationMatchId) || _.isUndefined(locationMatchId)) {
          return;
        }

        var locationMatch = $scope.locationMatches[locationMatchId];
        var selectedOrchestratorId = locationMatch.location.orchestratorId;

        // Affect selected location to all selected groups
        _.each($scope.selectedGroups, function(group, groupName) {
          $scope.groupsToLocations[groupName] = locationMatch.location.id;
          if ($scope.locationGroups[groupName].policies) {
            // Update location policy right now for bellow validation otherwise this will be done after
            // getting the rest response
            $scope.locationGroups[groupName].policies[0].locationId = locationMatch.location.id;
          }

        });


        // Now consider not selected groups
        _.each($scope.locationGroups, function(locationGrp) {
          if (!_.has($scope.selectedGroups, locationGrp.name) && locationGrp.policies) {
            var locationId = locationGrp.policies[0].locationId;
            var grpOrchId = $scope.locationMatches[locationId].orchestrator.id;
            if (selectedOrchestratorId == grpOrchId) {
              // valid case lets include this group in the request
              $scope.groupsToLocations[locationGrp.name] = locationId;
            } else {
              // invalidate mapping
              $scope.locationGroups[locationGrp.name].policies = null;
            }
          }
        });

        var configRequest = {
          orchestratorId: locationMatch.location.orchestratorId,
          groupsToLocations: $scope.groupsToLocations
        };

        deploymentTopologyServices.setLocationPolicies({
          appId: $scope.application.id,
          envId: $scope.environment.id
        }, angular.toJson(configRequest), function(response) {
          $scope.updateScopeDeploymentTopologyDTO(response.data);
          $scope.goToNextInvalidStep();
          // $state.go(thisMenu.nextStep.state);
        });

        $scope.selectedGroups = {};
      };
    }
  ]);
});
