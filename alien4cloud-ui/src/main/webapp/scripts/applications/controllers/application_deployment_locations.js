define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/deployment/directives/display_outputs');
  require('scripts/common/filters/inputs');
  require('scripts/applications/services/locations_matching_services');
  require('scripts/applications/services/deployment_configuration_services');

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
    ['$scope', 'authService', '$upload', 'applicationServices', 'toscaService', 'locationsMatchingServices', '$resource', '$http', '$translate', 'application', '$state', 'applicationEnvironmentServices', 'appEnvironments', 'toaster', '$filter', 'menu', 'deploymentConfigurationServices',
    function($scope, authService, $upload, applicationServices, toscaService, locationsMatchingServices, $resource, $http, $translate, applicationResult, $state, applicationEnvironmentServices, appEnvironments, toaster, $filter, menu, deploymentConfigurationServices) {
      
      var nodeMathingMenuItem;
      _.each(menu, function(menuItem) {
        if(menuItem.id === 'am.applications.detail.deployment.match') {
          nodeMathingMenuItem = menuItem;
        }
      });
      
      //enable or not the node matching menu
      var enableNodeMatchingMenu = function (){
        nodeMathingMenuItem.disabled = _.undefined($scope.deploymentContext.selectedLocation);
      }

      var refreshLocationMatching = function(){
        locationsMatchingServices.match({topologyId: $scope.topologyId}, function(result){
          $scope.deploymentContext.locationMatches = result.data;
          console.log('Result of the matching is: ',result);
        });
        enableNodeMatchingMenu();
//        console.log('in locations: ',$scope.topologyId);
      };
      
      var initSelectedLocation =function() {
        //TODO fetch the selected location for this topology if already defined
        delete $scope.deploymentContext.selectedLocation;
      }

      var initAll = function(){
       delete $scope.deploymentContext.locationMatches;
       initSelectedLocation();
      }
      
      //watch over the topologyId change, and refresh the location matching
      //Should we wath over the environment instead?
      $scope.$watch('topologyId', function(newValue, oldValue){
        console.log('new value: ', newValue);
        console.log('old value: ', oldValue);
        if(!_.defined(newValue)){
          //do nothing
          return;
        }
        
        if(newValue != oldValue){
          console.log('oldValue is '+oldValue+' And new value is '+newValue)
          console.log('topology changed')
          initAll(); 
          refreshLocationMatching();
        }else if(!_.has($scope, 'deploymentContext.locationMatches')) {
          //first run, match locations for the default selected environment.
          console.log('initializing')
          refreshLocationMatching();
        }
      });
      
      
      $scope.selectLocation = function(locationMatch){
        
        var configRequest = {
            environmentId: $scope.selectedEnvironment.id,
            locationId: locationMatch.location.id
        };
        
        deploymentConfigurationServices.update({appId: $scope.application.id}, angular.toJson(configRequest), function(result){
          console.log('Result of the matching is: ',result);
        });
        $scope.deploymentContext.selectedLocation = locationMatch.location;
        enableNodeMatchingMenu();
        console.log('Selected Location is: ', $scope.deploymentContext.selectedLocation.name)
        $state.go('applications.detail.deployment.match');
      }
      
      
      // checks if a location is the selected one for this deployment
      $scope.isLocationSelected = function(location){
        var selected = false;
        if(_.has($scope, "deploymentContext.selectedLocation")){
          selected = $scope.deploymentContext.selectedLocation.id === location.id
        }
        return selected;
      }
      
    }
  ]); //controller
}); //Define
