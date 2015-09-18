define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators', ['pascalprecht.translate']).controller('OrchestratorLocationResourceTemplateCtrl', [
    '$scope', 'locationResourcesService', 'locationResourcesPropertyService', 'locationResourcesCapabilityPropertyService',
    function($scope, locationResourcesService, locationResourcesPropertyService, locationResourcesCapabilityPropertyService) {
      $scope.getCapabilityPropertyDefinition = function(capabilityTypeId, capabilityPropertyName) {
        var capabilityType = $scope.context.locationResources.capabilityTypes[capabilityTypeId];
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
        }), function() {
          $scope.resourceTemplate.template.propertiesMap[propertyName].value = {value: propertyValue, definition: false};
        });
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
        }), function() {
          $scope.resourceTemplate.template.capabilitiesMap[capabilityName].value.propertiesMap[propertyName].value = {
            value: propertyValue,
            definition: false
          };
        });
      };
    }]);
});
