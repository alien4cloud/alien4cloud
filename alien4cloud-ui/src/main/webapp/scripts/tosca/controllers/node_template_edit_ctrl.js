define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('scripts/tosca/services/tosca_processor');

  modules.get('a4c-tosca').controller('a4cNodeTemplateEditCtrl', ['$scope', 'a4cToscaProcessor',
    function($scope, a4cToscaProcessor) {
      a4cToscaProcessor.processNodeType($scope.nodeType);
      a4cToscaProcessor.processNodeTemplate($scope.nodeTemplate);
      a4cToscaProcessor.processCapabilityTypes($scope.nodeCapabilityTypes);

      $scope.getCapabilityPropertyDefinition = function(capabilityTypeId, capabilityPropertyName) {
        var capabilityType = $scope.nodeCapabilityTypes[capabilityTypeId];
        return capabilityType.propertiesMap[capabilityPropertyName].value;
      };

      $scope.checkMapSize = function(map) {
        return _.defined(map) && Object.keys(map).length > 0;
      };

      $scope.updateProperty = function(propertyName, propertyValue) {
        var updatePromise = $scope.onPropertyUpdate({
          propertyName: propertyName,
          propertyValue: propertyValue
        });
        return updatePromise.then(function(response) {
          if (_.undefined(response.error)) { // update was performed on server side - impact js data.
            $scope.nodeTemplate.propertiesMap[propertyName].value = {value: propertyValue, definition: false};
          }
          return response; // dispatch response to property display
        });
      };

      $scope.updateCapabilityProperty = function(capabilityName, propertyName, propertyValue) {
        var updatePromise = $scope.onCapabilityPropertyUpdate({
          capabilityName: capabilityName,
          propertyName: propertyName,
          propertyValue: propertyValue
        });
        return updatePromise.then(function(response) {
          if (_.undefined(response.error)) {
            $scope.nodeTemplate.capabilitiesMap[capabilityName].value.propertiesMap[propertyName].value = {
              value: propertyValue,
              definition: false
            };
          }
          return response;
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
