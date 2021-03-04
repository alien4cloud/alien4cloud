define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/tosca/directives/policy_template_edit');

  modules.get('a4c-orchestrators').controller('OrchestratorLocationPolicyTemplateCtrl', ['$scope',
    function($scope) {

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

      $scope.canToggleTemplateMode = true;
      $scope.toggleTemplateMode = function() {
        $scope.updateLocationResource('onlyTemplate', !$scope.resourceTemplate.onlyTemplate);
        $scope.resourceTemplate.onlyTemplate = !$scope.resourceTemplate.onlyTemplate;
      }

      $scope.updateResourceProperty = function(propertyName, propertyValue) {
        return $scope.onPropertyUpdate({
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
    }]);
});
