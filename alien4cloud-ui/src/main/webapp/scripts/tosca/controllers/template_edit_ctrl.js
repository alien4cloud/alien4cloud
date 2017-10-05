// this is the common template edit controler. Essentially edit properties

define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('scripts/tosca/services/tosca_processor');

  modules.get('a4c-tosca').controller('a4cTemplateEditCtrl', ['$scope', 'a4cToscaProcessor',
    function($scope, a4cToscaProcessor) {

      a4cToscaProcessor.processInheritableToscaTypes($scope.type);
      a4cToscaProcessor.processTemplate($scope.template);
      a4cToscaProcessor.processInheritableToscaTypes($scope.resourceDataTypes);

      $scope.isService = _.defined($scope.isService) ? $scope.isService : false;

      $scope.getPropertyDefinition = function(propertyName) {
        return $scope.type.propertiesMap[propertyName].value;
      };

      $scope.getDataTypeForProperty = function(propertyName) {
        var propertyDefinition = $scope.getPropertyDefinition(propertyName);
        return $scope.resourceDataTypes[propertyDefinition.type];
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
            $scope.template.propertiesMap[propertyName].value = {value: propertyValue, definition: false};
          }
          return response; // dispatch response to property display
        });
      };

      $scope.canEditProperty = function(propertyName){
        return $scope.isPropertyEditable({
          propertyPath: {
            propertyName: propertyName
          }
        });
      };

    }]);
});
