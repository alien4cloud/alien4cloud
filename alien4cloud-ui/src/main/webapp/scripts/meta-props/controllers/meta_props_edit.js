define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-metas').controller('MetaPropertiesCtrl', ['$scope', 'propertiesServices', '$translate', 'applicationServices', 'cloudServices',
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
        if (_.defined($scope.application)) {
          return updateApplicationMetaProperty(updateApplicationPropertyObject);
        } else if (_.defined($scope.cloud)) {
          return updateCloudMetaProperty(updateApplicationPropertyObject);
        }
      };

      /* Return the current value */
      $scope.getPropertyValue = function(metaPropId) {
        if (_.defined($scope.application) && _.defined($scope.application.metaProperties)) {
          return $scope.application.metaProperties[metaPropId];
        } else if (_.defined($scope.cloud) && _.defined($scope.cloud.metaProperties)) {
          return $scope.cloud.metaProperties[metaPropId];
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
