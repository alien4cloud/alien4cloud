define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators', ['pascalprecht.translate']).controller('OrchestratorResourceTemplateCtrl', [
    '$scope', 'locationResourcesService', 'locationResourcesPropertyService', 'locationResourcesCapabilityPropertyService',
    function($scope, locationResourcesService, locationResourcesPropertyService, locationResourcesCapabilityPropertyService) {
      $scope.getCapabilityPropertyDefinition = function(capabilityType, capabilityPropertyName) {
        var capabilityType = $scope.context.locationResources.capabilityTypes[capabilityType];
        return capabilityType.propertiesMap[capabilityPropertyName].value;
      };
      $scope.checkMapSize = function(map) {
        return angular.isDefined(map) && map !== null && Object.keys(map).length > 0;
      };
      $scope.updateLocationResource = function(propertyName, propertyValue) {
        var updateLocationRequest = {};
        updateLocationRequest[propertyName] = propertyValue;
        locationResourcesService.update({
          orchestratorId: $scope.context.orchestrator.id,
          locationId: $scope.context.location.id,
          id: $scope.resourceTemplate.id
        }, angular.toJson(updateLocationRequest));
      };

      $scope.updateResourceProperty = function(propertyName, propertyValue) {
        locationResourcesPropertyService.save({
          orchestratorId: $scope.context.orchestrator.id,
          locationId: $scope.context.location.id,
          id: $scope.resourceTemplate.id
        }, angular.toJson({
          propertyName: propertyName,
          propertyValue: propertyValue
        }));
      };

      $scope.updateResourceCapabilityProperty = function(capabilityName, propertyName, propertyValue) {
        locationResourcesCapabilityPropertyService.save({
          orchestratorId: $scope.context.orchestrator.id,
          locationId: $scope.context.location.id,
          id: $scope.resourceTemplate.id,
          capabilityName: capabilityName
        }, angular.toJson({
          propertyName: propertyName,
          propertyValue: propertyValue
        }));
      };
    }]);
});