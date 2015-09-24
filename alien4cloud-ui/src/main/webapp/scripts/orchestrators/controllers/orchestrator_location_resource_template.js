define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-orchestrators', ['pascalprecht.translate']).controller('OrchestratorLocationResourceTemplateCtrl', [
    '$scope',
    function($scope) {
      $scope.getCapabilityPropertyDefinition = function(capabilityTypeId, capabilityPropertyName) {
        var capabilityType = $scope.resourceCapabilityTypes[capabilityTypeId];
        return capabilityType.propertiesMap[capabilityPropertyName].value;
      };

      $scope.checkMapSize = function(map) {
        return angular.isDefined(map) && map !== null && Object.keys(map).length > 0;
      };

      $scope.updateLocationResource = function(propertyName, propertyValue) {
        $scope.onUpdate({
          propertyName: propertyName,
          propertyValue: propertyValue
        });
      };

      function processResponsePromise(promise, callback) {
        if (!_.isEmpty(promise) && !_.isEmpty(promise.then)) {
          promise.then(function(response) {
            callback(response);
          });
        } else {
          callback();
        }
      }

      $scope.updateResourceProperty = function(propertyName, propertyValue) {
        var updatePromise = $scope.onPropertyUpdate({
          propertyName: propertyName,
          propertyValue: propertyValue
        });
        processResponsePromise(updatePromise, function(response) {
          $scope.resourceTemplate.template.propertiesMap[propertyName].value = {value: propertyValue, definition: false};
        });
      };

      $scope.updateResourceCapabilityProperty = function(capabilityName, propertyName, propertyValue) {
        var updatePromise = $scope.onCapabilityPropertyUpdate({
          capabilityName: capabilityName,
          propertyName: propertyName,
          propertyValue: propertyValue
        });
        processResponsePromise(updatePromise, function(response) {
          $scope.resourceTemplate.template.capabilitiesMap[capabilityName].value.propertiesMap[propertyName].value = {
            value: propertyValue,
            definition: false
          };
        });
      };
    }]);
});
