define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');


  require('scripts/orchestrators/controllers/orchestrator_location_resources');
  require('scripts/orchestrators/directives/orchestrator_location_resources');
  require('scripts/orchestrators/services/location_resources_security_service');
  require('scripts/users/directives/authorize_users');
  require('scripts/users/directives/authorize_groups');
  require('scripts/users/directives/authorize_apps');

  states.state('admin.orchestrators.details.locations.nodes', {
    url: '/nodes',
    templateUrl: 'views/orchestrators/orchestrator_location_nodes.html',
    controller: 'OrchestratorNodesCtrl',
    menu: {
      id: 'menu.orchestrators.locations.nodes',
      state: 'admin.orchestrators.details.locations.nodes',
      key: 'ORCHESTRATORS.LOCATIONS.ON_DEMAND_RESOURCES',
      icon: 'fa fa-cubes',
      priority: 200
    }
  });

  modules.get('a4c-orchestrators').controller('OrchestratorNodesCtrl', ['$scope', '$resource', 'locationResourcesProcessor', '$uibModal', 'locationResourcesSecurityService',
    function($scope, $resource, locationResourcesProcessor, $uibModal, locationResourcesSecurityService) {

      function removeGeneratedResources() {
          _.remove($scope.context.locationResources.nodeTemplates, function(locationResource){
            return locationResource.generated;
          });
        }

      $scope.autoConfigureResources = function(){
        $scope.autoConfiguring = true;
        $resource('rest/latest/orchestrators/'+$scope.context.orchestrator.id+'/locations/'+$scope.context.location.id+'/resources/auto-configure').get({},
          function(result){
            if(_.undefined($scope.context.locationResources.nodeTemplates)){
              $scope.context.locationResources.nodeTemplates = [];
            }
            removeGeneratedResources();
            if(!_.isEmpty(result.data)){
              locationResourcesProcessor.processLocationResourceTemplates(result.data);
              $scope.context.locationResources.nodeTemplates = $scope.context.locationResources.nodeTemplates.concat(result.data);
            }
            $scope.autoConfiguring = false;
          }, function(){
            $scope.autoConfiguring=false;
          });
      };


      /************************************
      *  For authorizations directives
      /************************************/

      var params = {
        orchestratorId: $scope.context.orchestrator.id,
        locationId: $scope.context.location.id
      };

      $scope.buildSecuritySearchConfig = function(subject){
        return {
          url: 'rest/latest/orchestrators/:orchestratorId/locations/:locationId/security/' + subject + '/search',
          useParams: true,
          params: _.clone(params, true)
        };
      };

      // *****************************************************************************
      // USERS
      // *****************************************************************************

      $scope.processUserAction = function (action, result) {
        var request = {
          'resources':  Object.keys($scope.context.selectedResourceTemplates)
        };
        request[action] = _.map(result.subjects, 'username');
        locationResourcesSecurityService.bulkUsers(_.merge(params, {force: result.force}), angular.toJson(request), function(successResponse) {
          console.log(successResponse);
          //TODO: check if an error occur and add a refresh
        });
      };

      // *****************************************************************************
      // GROUPS
      // *****************************************************************************

      $scope.processGroupAction = function (action, result) {
          var request = {
            'resources':  Object.keys($scope.context.selectedResourceTemplates)
          };
          request[action] = _.map(result.subjects, 'id');
          locationResourcesSecurityService.bulkGroups(_.merge(params, {force:result.force}), angular.toJson(request), function(successResponse) {
            console.log(successResponse);
            //TODO: check if an error occur and add a refresh
          });
      };

      // *****************************************************************************
      // APPLICATIONS / ENVIRONMENTS
      // *****************************************************************************

      $scope.processAppsAction = function (action, result) {
        var request = result.subjects;
        request.resources =  Object.keys($scope.context.selectedResourceTemplates);
        if (action === 'revoke') {
          request.applicationsToDelete = request.applicationsToAdd;
          request.environmentsToDelete = request.environmentsToAdd;
          request.environmentTypesToDelete = request.environmentTypesToAdd;
          delete request.applicationsToAdd;
          delete request.environmentsToAdd;
          delete request.environmentTypesToAdd;
        }
        locationResourcesSecurityService.updateEnvironmentsPerApplicationBatch.grant(_.merge(params, {force:result.force}), angular.toJson(request), function(successResponse) {
          console.log(successResponse);
          //TODO: check if an error occur and add a refresh
        });
      };
    }
  ]);
}); // define
