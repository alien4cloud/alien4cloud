define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-orchestrators', ['pascalprecht.translate']).controller('OrchestratorLocationResourceTemplateCtrl', [
    '$scope', '$translate',
    function($scope, $translate) {
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
        if (!_.isEmpty(promise) && promise.hasOwnProperty('then')) {
          return promise.then(function(response) {
             callback(response);
             return response;
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
        return processResponsePromise(updatePromise, function(response) {
          if (_.undefined(response.error)) {
            $scope.resourceTemplate.template.capabilitiesMap[capabilityName].value.propertiesMap[propertyName].value = {
              value: propertyValue,
              definition: false
            };
          } else {
            console.log(response.error.message);
          }
        });
      };
      
      $scope.canEditProperty = function(propertyName){
        return $scope.isPropertyEditable({
          propertyPath: {
            propertyName: propertyName
          }
        });
      };
      $scope.canEditCapabilityProperty = function(capabilityName, propertyName){
        return $scope.isPropertyEditable({
          propertyPath: {
            capabilityName: capabilityName,
            propertyName: propertyName
          }
        });
      };
    }]);
});
