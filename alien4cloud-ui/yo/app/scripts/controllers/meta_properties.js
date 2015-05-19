/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('MetaPropertiesCtrl', ['$scope', 'propertiesServices', '$translate', 'applicationServices',
  function($scope, propertiesServices, $translate, applicationServices) {

    $scope.updateProperties = function(propertyDefinitionId, value, metaProperties) {
      var updateApplicationPropertyObject = {
        'propertyDefinitionId': propertyDefinitionId,
        'propertyValue': value
      };

      return applicationServices.upsertProperty({
        applicationId: $scope.application.id
      }, angular.toJson(updateApplicationPropertyObject), function(response) {
        if (!response.error) {
          metaProperties[propertyDefinitionId] = value;
        }
      });
    };

  }
]);
