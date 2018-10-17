define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/controllers/orchestrator_location_new');
  require('scripts/orchestrators/controllers/orchestrator_location_config');
  require('scripts/orchestrators/controllers/orchestrator_location_nodes');
  require('scripts/orchestrators/controllers/orchestrator_location_policies');
  require('scripts/orchestrators/controllers/orchestrator_location_secret');
  require('scripts/orchestrators/controllers/orchestrator_location_modifiers');
  require('scripts/orchestrators/controllers/orchestrator_location_metaprops');
  require('scripts/orchestrators/controllers/orchestrator_location_security');
  require('scripts/orchestrators/services/orchestrator_location_service');
  require('scripts/orchestrators/services/location_resources_processor');

  states.state('admin.orchestrators.details.locations', {
    url: '/locations',
    templateUrl: 'views/orchestrators/orchestrator_locations.html',
    controller: 'OrchestratorLocationsCtrl',
    menu: {
      id: 'menu.orchestrators.locations',
      state: 'admin.orchestrators.details.locations',
      key: 'ORCHESTRATORS.NAV.LOCATIONS',
      icon: 'fa fa-cloud',
      priority: 400
    }
  });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('OrchestratorLocationsCtrl',
    ['$scope', '$uibModal', '$http', 'locationService', 'orchestrator', 'menu', 'locationResourcesProcessor', '$translate', '$state', 'breadcrumbsService',
      function($scope, $uibModal, $http, locationService, orchestrator, menu, locationResourcesProcessor, $translate, $state, breadcrumbsService) {

        breadcrumbsService.putConfig({
          state: 'admin.orchestrators.details.locations',
          text: function() {
            return $translate.instant('ORCHESTRATORS.NAV.LOCATIONS');
          }
        });

        $scope.envTypes = ['OTHER', 'DEVELOPMENT', 'INTEGRATION_TESTS', 'USER_ACCEPTANCE_TESTS', 'PRE_PRODUCTION', 'PRODUCTION'];
        $scope.orchestrator = orchestrator;
        $scope.menu = menu;
        var locationSupport;
        $scope.uiModel={};
        // query to get the location support informations for the orchestrator.

        $http.get('rest/latest/orchestrators/' + orchestrator.id + '/locationsupport').then(function(response) {
          if (_.defined(response.data.data)) {
            locationSupport = response.data.data;
            $scope.multipleLocations = locationSupport.multipleLocations;
            $scope.locationTypes = locationSupport.types;
          }
        });

        // get all locations (no need for paginations as we never expect a huge number of locations for an orchestrator)
        function updateLocations(toSelectName) {
          locationService.get({orchestratorId: orchestrator.id}, function(result) {
            $scope.locationsDTOs = result.data;
            if ($scope.locationsDTOs.length > 0) {
              var toSelect = toSelectName ? _.find($scope.locationsDTOs, {'location':{'name':toSelectName}}): undefined;
              $scope.switchLocation(toSelect|| $scope.locationsDTOs[0]);
              $state.go('admin.orchestrators.details.locations.config');
            }
          });
        }

        function reloadLocation(location) {
            locationService.get({orchestratorId: orchestrator.id, locationId: location.location.id},function(result) {
                location = result.data;
                for (var i = 0 ; i < $scope.locationsDTOs.length ; i++) {
                    if ($scope.locationsDTOs[i].location.id == location.location.id) {
                        $scope.locationsDTOs[i] = location;
                    }
                    $scope.switchLocation(location);
                }
            });
        }

        if(orchestrator.state === 'CONNECTED') {
          updateLocations();
        }

        $scope.switchLocation = function(location) {
          locationResourcesProcessor.processLocationResources(location.resources);
          $scope.uiModel.locationDTO = location;
          $scope.context.location = location.location;
          $scope.context.locationResources = location.resources;
        }

        $scope.selectLocation = function(location) {
          reloadLocation(location);
        };

        $scope.deleteLocation = function(location){
          locationService.delete({orchestratorId: orchestrator.id, locationId: location.location.id}, null, function(result){
            if(result.data === true){
              delete $scope.uiModel.locationDTO;
              updateLocations();
            }
          });
        };

        $scope.updateLocation = function(request) {
          if (request.name !== $scope.uiModel.locationDTO.location.name || request.environmentType !== $scope.uiModel.locationDTO.location.environmentType ) {
            return locationService.update({orchestratorId: orchestrator.id, locationId: $scope.uiModel.locationDTO.location.id}, angular.toJson(request)).$promise.then(
              function() {}, // Success
              function(errorResponse) {
                return $translate.instant('ERRORS.' + errorResponse.data.error.code);
              });
          }
        };

        $scope.openNewModal = function() {
          var modalInstance = $uibModal.open({
            templateUrl: 'views/orchestrators/orchestrator_location_new.html',
            controller: 'NewLocationController',
            resolve: {
              locationTypes: function() {
                return locationSupport.types;
              }
            }
          });

          modalInstance.result.then(function(newLocation) {
            locationService.create({orchestratorId: orchestrator.id}, angular.toJson(newLocation), function() {
              updateLocations(newLocation.name);
            });
          });
        };
      }
    ]); // controller
}); // define
