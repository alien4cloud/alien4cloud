/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('MetaPropertiesCtrl', ['$scope', 'propertiesServices', '$translate', 'applicationServices', 'cloudServices',
  function($scope, propertiesServices, $translate, applicationServices, cloudServices) {

    $scope.updateProperties = function(type, propertyDefinitionId, value) {
      var updateApplicationPropertyObject = {
        'definitionId': propertyDefinitionId,
        'value': value
      };
      if (UTILS.isDefinedAndNotNull($scope.application)) {
        $scope.updateApplicationMetaProperties(updateApplicationPropertyObject);
      } else if (UTILS.isDefinedAndNotNull($scope.cloud)) {
        $scope.updateCloudMetaProperties(updateApplicationPropertyObject);
      }
    };

    $scope.updateApplicationMetaProperties = function(updateApplicationPropertyObject) {
      return applicationServices.upsertProperty({
        applicationId: $scope.application.id
      }, angular.toJson(updateApplicationPropertyObject), function(response) {
        if (!response.error) {
          $scope.application.metaProperties[updateApplicationPropertyObject.definitionId] = updateApplicationPropertyObject.value;
        }
      });
    };

    $scope.updateCloudMetaProperties = function(updateApplicationPropertyObject) {
      return cloudServices.upsertProperty({
        cloudId: $scope.cloud.id
      }, angular.toJson(updateApplicationPropertyObject), function(response) {
        if (!response.error) {
          $scope.cloud.metaProperties[updateApplicationPropertyObject.definitionId] = updateApplicationPropertyObject.value;
        }
      });
    };

    $scope.getPropertyValue = function(metaPropId) {
      if (UTILS.isDefinedAndNotNull($scope.application)) {
        return $scope.application.metaProperties[metaPropId];
      } else if (UTILS.isDefinedAndNotNull($scope.cloud)) {
        return $scope.cloud.metaProperties[metaPropId];
      }
    };

  }
]);
