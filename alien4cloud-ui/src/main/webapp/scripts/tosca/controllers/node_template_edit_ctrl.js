define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('scripts/tosca/services/tosca_processor');
  require('scripts/tosca/services/relationship_type_quicksearch_service');

  require('scripts/tosca/controllers/template_edit_ctrl');

  modules.get('a4c-tosca').controller('a4cNodeTemplateEditCtrl', ['$controller', '$scope', 'a4cToscaProcessor', 'relationshipTypeQuickSearchService',
    function($controller, $scope, a4cToscaProcessor, relationshipTypeQuickSearchService) {

      // first load default template edit controller
      $controller('a4cTemplateEditCtrl', {
        $scope: $scope,
        a4cToscaProcessor: a4cToscaProcessor
      });

      a4cToscaProcessor.processInheritableToscaTypes($scope.nodeCapabilityTypes);
      a4cToscaProcessor.processNodeTemplate($scope.template);

      $scope.getCapabilityPropertyDefinition = function(capabilityTypeId, capabilityPropertyName) {
        var capabilityType = $scope.nodeCapabilityTypes[capabilityTypeId];
        return capabilityType.propertiesMap[capabilityPropertyName].value;
      };

      $scope.getDataTypeForCapabilityProperty = function(capabilityTypeId, capabilityPropertyName) {
        var propertyDefinition = $scope.getCapabilityPropertyDefinition(capabilityTypeId, capabilityPropertyName);
        return $scope.resourceDataTypes[propertyDefinition.type];
      };

      $scope.relationshipTypeQuickSearchHandler = {
        'doQuickSearch': relationshipTypeQuickSearchService.doQuickSearch,
        'waitBeforeRequest': 500,
        'minLength': 3
      };

      $scope.updateHalfRelationshipType = function(name, relationshipTypeId) {
        if(relationshipTypeId === '') {
            relationshipTypeId = null;
        }
        var updatePromise = $scope.onHalfRelationshipTypeUpdate({
          type: 'capability',
          name: name,
          relationshipTypeId: relationshipTypeId
        });
        return updatePromise.then(function(response) {
          if (_.undefined(response.error)) { // update was performed on server side - impact js data.
            $scope.capabilitiesRelationshipTypes[name] = relationshipTypeId;
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
            $scope.template.capabilitiesMap[capabilityName].value.propertiesMap[propertyName].value = {
              value: propertyValue,
              definition: false
            };
          }
          return response;
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
