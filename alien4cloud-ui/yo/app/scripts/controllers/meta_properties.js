/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('MetaPropertiesCtrl', ['$scope', 'propertiesServices', '$translate', 'applicationServices', 'cloudServices',
  function($scope, propertiesServices, $translate, applicationServices, cloudServices) {

    var updateApplicationMetaProperties = function(updateApplicationPropertyObject) {
      return applicationServices.upsertProperty({
        applicationId: $scope.application.id
      }, angular.toJson(updateApplicationPropertyObject), function(response) {
        if (!response.error) {
          $scope.application.metaProperties[updateApplicationPropertyObject.definitionId] = updateApplicationPropertyObject.value;
        }
      });
    };

    var updateCloudMetaProperties = function(updateApplicationPropertyObject) {
      return cloudServices.upsertProperty({
        cloudId: $scope.cloud.id
      }, angular.toJson(updateApplicationPropertyObject), function(response) {
        if (!response.error) {
          $scope.cloud.metaProperties[updateApplicationPropertyObject.definitionId] = updateApplicationPropertyObject.value;
        }
      });
    };

    /* Call the appropriate service */
    $scope.updateProperties = function(type, propertyDefinitionId, value) {
      var updateApplicationPropertyObject = {
        'definitionId': propertyDefinitionId,
        'value': value
      };
      // In future, we need to add component here
      if (UTILS.isDefinedAndNotNull($scope.application)) {
        updateApplicationMetaProperties(updateApplicationPropertyObject);
      } else if (UTILS.isDefinedAndNotNull($scope.cloud)) {
        updateCloudMetaProperties(updateApplicationPropertyObject);
      }
    };

    /* Return the current value */
    $scope.getPropertyValue = function(metaPropId) {
      if (UTILS.isDefinedAndNotNull($scope.application) && UTILS.isDefinedAndNotNull($scope.application.metaProperties)) {
        return $scope.application.metaProperties[metaPropId];
      } else if (UTILS.isDefinedAndNotNull($scope.cloud) && UTILS.isDefinedAndNotNull($scope.cloud.metaProperties)) {
        return $scope.cloud.metaProperties[metaPropId];
      }
    };

  }
]);
