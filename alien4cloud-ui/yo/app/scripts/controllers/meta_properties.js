/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('MetaPropertiesCtrl', ['$scope', 'propertiesServices', '$translate', 'applicationServices', 'cloudServices',
  function($scope, propertiesServices, $translate, applicationServices, cloudServices) {

    var updateApplicationMetaProperty = function(updateApplicationPropertyObject) {
      return applicationServices.upsertProperty({
        applicationId: $scope.application.id
      }, angular.toJson(updateApplicationPropertyObject), function(response) {
        if (!response.error) {
          $scope.application.metaProperties[updateApplicationPropertyObject.definitionId] = updateApplicationPropertyObject.value;
        }
      }).$promise;
    };

    var updateCloudMetaProperty = function(updateApplicationPropertyObject) {
      return cloudServices.upsertProperty({
        cloudId: $scope.cloud.id
      }, angular.toJson(updateApplicationPropertyObject), function(response) {
        if (!response.error) {
          if (!$scope.cloud.hasOwnProperty('metaProperties')) {
            $scope.cloud.metaProperties = {};
          }
          $scope.cloud.metaProperties[updateApplicationPropertyObject.definitionId] = updateApplicationPropertyObject.value;
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
      if (UTILS.isDefinedAndNotNull($scope.application)) {
        return updateApplicationMetaProperty(updateApplicationPropertyObject);
      } else if (UTILS.isDefinedAndNotNull($scope.cloud)) {
        return updateCloudMetaProperty(updateApplicationPropertyObject);
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

    /* By default, we collapse the meta-properties tab*/
    $scope.initCollapse = function() {
      if (UTILS.isDefinedAndNotNull($scope.collapse)) {
        $scope.isMetaPropsCollapsed = $scope.collapse;
      } else {
        $scope.isMetaPropsCollapsed = true;
      }
    };

  }
]);
