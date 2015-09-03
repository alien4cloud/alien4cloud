define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators', ['pascalprecht.translate']).controller('OrchestratorResourceTemplateCtrl', ['$scope', function($scope) {
    $scope.getCapabilityPropertyDefinition = function(capabilityType, capabilityPropertyName) {
      var capabilityType = $scope.context.locationResources.capabilityTypes[capabilityType];
      return capabilityType.propertiesMap[capabilityPropertyName].value;
    };
    $scope.checkMapSize = function(map) {
      return angular.isDefined(map) && map !== null && Object.keys(map).length > 0;
    };
  }]);
});