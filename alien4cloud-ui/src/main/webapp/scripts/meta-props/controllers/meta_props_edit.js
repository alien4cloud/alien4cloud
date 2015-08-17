define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-metas').controller('MetaPropertiesCtrl', ['$scope', 'propertiesServices', '$translate', 'applicationServices', 'orchestratorPropertiesServices',
    function($scope, propertiesServices, $translate, applicationServices, orchestratorPropertiesServices) {

      var updateApplicationMetaProperty = function(updateApplicationPropertyObject) {
        return applicationServices.upsertProperty({
          applicationId: $scope.application.id
        }, angular.toJson(updateApplicationPropertyObject), function(response) {
          if (!response.error) {
            $scope.application.metaProperties[updateApplicationPropertyObject.definitionId] = updateApplicationPropertyObject.value;
          }
        }).$promise;
      };

      var updateOrchestratorMetaProperty = function(updateOrchestratorPropertyObject) {
        return orchestratorPropertiesServices.upsertProperty({
          id: $scope.orchestrator.id
        }, angular.toJson(updateOrchestratorPropertyObject), function(response) {
          if (!response.error) {
            if (!$scope.orchestrator.hasOwnProperty('metaProperties')) {
              $scope.orchestrator.metaProperties = {};
            }
            $scope.orchestrator.metaProperties[updateOrchestratorPropertyObject.definitionId] = updateOrchestratorPropertyObject.value;
          }
        }).$promise;
      };

      /* Call the appropriate service */
      $scope.updateProperty = function(type, propertyDefinitionId, value) {
        var updateApplicationPropertyObject = {
          'definitionId': propertyDefinitionId,
          'value': value
        };
        // In future, we need to add component here
        if (_.defined($scope.application)) {
          return updateApplicationMetaProperty(updateApplicationPropertyObject);
        } else if (_.defined($scope.orchestrator)) {
          return updateOrchestratorMetaProperty(updateApplicationPropertyObject);
        }
      };

      /* Return the current value */
      $scope.getPropertyValue = function(metaPropId) {
        if (_.defined($scope.application) && _.defined($scope.application.metaProperties)) {
          return $scope.application.metaProperties[metaPropId];
        } else if (_.defined($scope.orchestrator) && _.defined($scope.orchestrator.metaProperties)) {
          return $scope.orchestrator.metaProperties[metaPropId];
        }
      };

      /* By default, we collapse the meta-properties tab*/
      $scope.initCollapse = function() {
        if (_.defined($scope.collapse)) {
          $scope.isMetaPropsCollapsed = $scope.collapse;
        } else {
          $scope.isMetaPropsCollapsed = true;
        }
      };
    }
  ]);
}); // define
