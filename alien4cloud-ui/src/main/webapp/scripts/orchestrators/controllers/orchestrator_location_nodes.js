define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');


  require('scripts/orchestrators/controllers/orchestrator_location_resources');
  require('scripts/orchestrators/directives/orchestrator_location_resources');
  require('scripts/orchestrators/services/location_resources_security_service');
  require('scripts/users/controllers/users_authorization_modal_ctrl');
  require('scripts/users/controllers/groups_authorization_modal_ctrl');

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

      $scope.buildSecuritySearchConfig = function(subject){
        return {
          url: 'rest/latest/orchestrators/:orchestratorId/locations/:locationId/security/' + subject + '/search',
          useParams: true,
          params: {
            orchestratorId: $scope.context.orchestrator.id,
            locationId: $scope.context.location.id,
          }
        };
      };

      var params = {
        orchestratorId: $scope.context.orchestrator.id,
        locationId: $scope.context.location.id,
      };

      // *****************************************************************************
      // USERS
      // *****************************************************************************

      $scope.openUsersdModal = function (service) {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/users/users_authorization_popup.html',
          controller: 'UsersAuthorizationModalCtrl',
          resolve:{
            searchConfig:  $scope.buildSecuritySearchConfig('users'),
            authorizedUsers: function() { return $scope.authorizedUsers; }
          }
        });

        modalInstance.result.then(function (users) {
          var request = {
            'resources':  Object.keys($scope.context.selectedResourceTemplates),
            'subjects': _.map(users, function (user) {return user.username;})
          };

          if (service === 'grant') {
            locationResourcesSecurityService.grantUsersBatch[service](params, angular.toJson(request), function(successResponse) {
              console.log(successResponse);
              //TODO: check if an error occur and add a refresh
            });
          } else if (service === 'revoke') {
            locationResourcesSecurityService.revokeUsersBatch(params, request);
          }

        });
      };

      // *****************************************************************************
      // GROUPS
      // *****************************************************************************

      $scope.openGroupsdModal = function (service) {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/users/groups_authorization_popup.html',
          controller: 'GroupsAuthorizationModalCtrl',
          resolve:{
            searchConfig:  $scope.buildSecuritySearchConfig('groups'),
            authorizedGroups: function(){ return $scope.authorizedGroups; }
          }
        });

        modalInstance.result.then(function (groups) {
          var request = {
            'resources':  Object.keys($scope.context.selectedResourceTemplates),
            'subjects': _.map(groups, function (group) {return group.id;})
          };

          if (service === 'grant') {
            locationResourcesSecurityService.grantGroupsBatch[service](params, angular.toJson(request), function(successResponse) {
              console.log(successResponse);
              //TODO: check if an error occur and add a refresh
            });
          } else if (service === 'revoke') {
            locationResourcesSecurityService.revokeGroupsBatch(params, request);
          }

        });
      };


    }
  ]);
}); // define
