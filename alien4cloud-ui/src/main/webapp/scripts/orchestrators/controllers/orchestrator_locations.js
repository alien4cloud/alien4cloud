define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/controllers/orchestrator_location_new');
  require('scripts/orchestrators/controllers/orchestrator_location_config');
  require('scripts/orchestrators/controllers/orchestrator_location_nodes');
  require('scripts/orchestrators/controllers/orchestrator_location_services');
  require('scripts/orchestrators/controllers/orchestrator_location_policies');
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
    ['$scope', '$uibModal', '$http', 'locationService', 'orchestrator', 'menu', 'locationResourcesProcessor', '$translate', '$state',
      function($scope, $uibModal, $http, locationService, orchestrator, menu, locationResourcesProcessor, $translate, $state) {
        $scope.envTypes = ['OTHER', 'DEVELOPMENT', 'INTEGRATION_TESTS', 'USER_ACCEPTANCE_TESTS', 'PRE_PRODUCTION', 'PRODUCTION'];
        $scope.orchestrator = orchestrator;
        $scope.menu = menu;
        var locationSupport;
        // query to get the location support informations for the orchestrator.

        $http.get('rest/latest/orchestrators/' + orchestrator.id + '/locationsupport').then(function(response) {
          if (_.defined(response.data)) {
            locationSupport = response.data;
            $scope.multipleLocations = locationSupport.multipleLocations;
            $scope.locationTypes = locationSupport.types;
          }
        });

        // get all locations (no need for paginations as we never expect a huge number of locations for an orchestrator)
        function updateLocations() {
          locationService.get({orchestratorId: orchestrator.id}, function(result) {
            $scope.locationsDTOs = result.data;
            if ($scope.locationsDTOs.length > 0 && _.isUndefined($scope.locationDTO)) {
              // For the moment show only first location
              $scope.selectLocation($scope.locationsDTOs[0]);
              $state.go('admin.orchestrators.details.locations.config');
            }
          });
        }

        if(orchestrator.state === 'CONNECTED') {
          updateLocations();
        }

        $scope.selectLocation = function(location) {
          locationResourcesProcessor.processLocationResources(location.resources);
          $scope.locationDTO = location;
          $scope.context.location = location.location;
          $scope.context.locationResources = location.resources;
          $scope.context.configurationTypes = _.values(location.resources.configurationTypes);
          $scope.context.nodeTypes = _.values(location.resources.nodeTypes);
        };

        $scope.deleteLocation = function(location){
          locationService.delete({orchestratorId: orchestrator.id, locationId: location.location.id}, null, function(result){
            if(result.data === true){
              delete $scope.locationDTO;
              updateLocations();
            }
          });
        };

        $scope.updateLocation = function(request) {
          if (request.name !== $scope.locationDTO.location.name || request.environmentType !== $scope.locationDTO.location.environmentType ) {
            return locationService.update({orchestratorId: orchestrator.id, locationId: $scope.locationDTO.location.id}, angular.toJson(request)).$promise.then(
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
              updateLocations();
            });
          });
        };
      }
    ]); // controller
}); // define
