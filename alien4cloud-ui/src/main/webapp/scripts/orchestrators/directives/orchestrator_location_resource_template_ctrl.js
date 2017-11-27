define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/tosca/directives/node_template_edit');

  modules.get('a4c-orchestrators').controller('OrchestratorLocationResourceTemplateCtrl', ['$scope',
    function($scope) {
      $scope.getCapabilityPropertyDefinition = function(capabilityTypeId, capabilityPropertyName) {
        var capabilityType = $scope.resourceCapabilityTypes[capabilityTypeId];
        return capabilityType.propertiesMap[capabilityPropertyName].value;
      };

      $scope.checkMapSize = function(map) {
        return _.defined(map) && Object.keys(map).length > 0;
      };

      $scope.isObjectEmpty = function(obj) {
        if (_.undefined(obj)) {
          return true;
        }
        for (var i in obj) {
          if (obj.hasOwnProperty(i)) {
            return false;
          }
        }
        return true;
      };

      $scope.updateLocationResource = function(propertyName, propertyValue) {
        $scope.onUpdate({
          propertyName: propertyName,
          propertyValue: propertyValue
        });
      };

      $scope.updateResourceProperty = function(propertyName, propertyValue) {
        return $scope.onPropertyUpdate({
          propertyName: propertyName,
          propertyValue: propertyValue
        });
      };

      $scope.updateResourceCapabilityProperty = function(capabilityName, propertyName, propertyValue) {
        return $scope.onCapabilityPropertyUpdate({
          capabilityName: capabilityName,
          propertyName: propertyName,
          propertyValue: propertyValue
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

      $scope.updatePortabilityProperty = function(propertyName, propertyValue) {
        var updatePromise = $scope.onPortabilityPropertyUpdate({
          propertyName: propertyName,
          propertyValue: propertyValue
        });
        return updatePromise.then(function(response) {
          if (_.undefined(response.error)) { // update was performed on server side - impact js data.
            $scope.resourceTemplate.template.portability[propertyName] = propertyValue;
          }
          return response; // dispatch response to property display
        });
      };

      $scope.isSecretEditable = function() {
        return true;
      };

    }]);
});
