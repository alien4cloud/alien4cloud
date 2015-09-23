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
    ['$scope', '$modal', '$http', 'locationService', 'orchestrator', 'menu', 'locationResourcesProcessor',
      function($scope, $modal, $http, locationService, orchestrator, menu, locationResourcesProcessor) {
        $scope.envTypes = ['OTHER', 'DEVELOPMENT', 'INTEGRATION_TESTS', 'USER_ACCEPTANCE_TESTS', 'PRE_PRODUCTION', 'PRODUCTION'];
        $scope.orchestrator = orchestrator;
        $scope.menu = menu;
        var locationSupport;
        // query to get the location support informations for the orchestrator.

        $http.get('rest/orchestrators/' + orchestrator.id + '/locationsupport').success(function(response) {
          if (_.defined(response.data)) {
            locationSupport = response.data;
            $scope.multipleLocations = locationSupport.multipleLocations;
            $scope.locationTypes = locationSupport.types;
          }
        });

        // get all locations (no need for paginations as we never expect a huge number of locations for an orchestrator)
        function updateLocations() {
          locationService.get({orchestratorId: orchestrator.id}, function(result) {
            $scope.locations = result.data;
            if ($scope.locations.length > 0 && _.isUndefined($scope.location)) {
              // For the moment show only first location
              $scope.selectLocation($scope.locations[0]);
            }
          });
        }

        if(orchestrator.state === 'CONNECTED') {
          updateLocations();
        }

        $scope.selectLocation = function(location) {
          locationResourcesProcessor.process(location.resources);
          $scope.location = location;
          $scope.context.location = $scope.location.location;
          $scope.context.locationResources = location.resources;
          $scope.context.configurationTypes = _.values(location.resources.configurationTypes);
          $scope.context.nodeTypes = _.values(location.resources.nodeTypes);
        };
        
        $scope.deleteLocation = function(location){
          locationService.delete({orchestratorId: orchestrator.id, locationId: location.location.id}, null, function(result){
            if(result.data == true){
              delete $scope.location;
              updateLocations();
            }
          });
        }

        $scope.openNewModal = function() {
          var modalInstance = $modal.open({
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
