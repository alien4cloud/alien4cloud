define(function (require) {
  'use strict';
  
  var angular = require('angular');
  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  
  states.state('admin.orchestrators.details.locations.secret', {
    url: '/secret',
    templateUrl: 'views/orchestrators/orchestrator_location_secret.html',
    controller: 'OrchestratorLocationSecretCtrl',
    menu: {
      id: 'menu.orchestrators.locations.secret',
      state: 'admin.orchestrators.details.locations.secret',
      key: 'ORCHESTRATORS.LOCATIONS.SECRETS.SECRET',
      icon: 'fa fa-key',
      priority: 500,
      active: true
    }
  });
  
  modules.get('a4c-orchestrators').controller('OrchestratorLocationSecretCtrl', ['$scope', 'locationSecretService',
    function ($scope, locationSecretService) {
      
      $scope.currentPluginConfiguration = {
        pluginName: _.get($scope, 'uiModel.locationDTO.secretProviderConfigurations.currentConfiguration.pluginName'),
        configuration: _.get($scope, 'uiModel.locationDTO.secretProviderConfigurations.currentConfiguration.configuration')
      };
      
      // populate the scope with the ncessary for location policies resources security
      //locationResourcesSecurity('rest/latest/orchestrators/:orchestratorId/locations/:locationId/policies', $scope);
      $scope.saveConfiguration = function () {
        return locationSecretService.save({
          orchestratorId: $scope.orchestrator.id,
          locationId: $scope.uiModel.locationDTO.location.id
        }, angular.toJson($scope.currentPluginConfiguration), function () {
          $scope.uiModel.locationDTO.secretProviderConfigurations.currentConfiguration = $scope.currentPluginConfiguration;
        }).$promise;
      };
      
      $scope.deleteConfiguration = function () {
        locationSecretService.delete({
          orchestratorId: $scope.orchestrator.id,
          locationId: $scope.uiModel.locationDTO.location.id
        }, undefined, function () {
          $scope.currentPluginConfiguration = {
            pluginName: undefined,
            configuration: undefined
          };
          delete $scope.uiModel.locationDTO.secretProviderConfigurations.currentConfiguration;
        });
      };
    }
  ]);
}); // define
