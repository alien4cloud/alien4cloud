define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/properties_services');

  modules.get('a4c-common').controller('SecretDisplayCtrl', ['$scope', '$translate', '$uibModal',
    function($scope, $translate, $uibModal) {

      // Init the directive id
      if (_.defined($scope.capabilityName)) {
        $scope.id = _.defined($scope.id) ? $scope.id : $scope.capabilityName + '-' + $scope.propertyName;
      } else if (_.defined($scope.relationshipName)) {
        $scope.id = _.defined($scope.id) ? $scope.id : $scope.relationshipName + '-' + $scope.propertyName;
      } else {
        $scope.id = _.defined($scope.id) ? $scope.id : $scope.propertyName;
      }

      var check = function(scope, secretPath) {
        // The capablity "scalable" can not become a secret
        if ("scalable" === scope.capabilityName) {
          return "Can not set the capablity scalable with a secret.";
        }
        // The property "component_version" can not become a secret
        if ("component_version" === scope.propertyName) {
          return "Can not set the component_version with a secret.";
        }
        if (_.undefined(secretPath)) {
          return "";
        }
        if (secretPath === "") {
          return "The path can not be null.";
        }
        return undefined;
      };

      $scope.save = function(secretPath) {
        var error = check($scope, secretPath);
        if (_.defined(error)) {
          return error;
        }
        return $scope.onSave({
          secretPath: secretPath,
          propertyName: $scope.propertyName,
          propertyValue: $scope.propertyValue,
          capabilityName: $scope.capabilityName,
          relationshipName: $scope.relationshipName});
      };

      /*
      * A listener for focusing on the text editor.
      */
      $scope.$on('focus-on', function(event, object) {
        var propertyName = event.currentScope.propertyName;
        var capabilityName = event.currentScope.capabilityName;
        var relationshipName = event.currentScope.relationshipName;


        if (propertyName === object.propertyName && capabilityName === object.capabilityName && relationshipName === object.relationshipName) {
          // Trigger the click on text editor
          setTimeout(function () {
            // Use setTimeout because the UI element is not yet loaded in the page.
            if (_.defined(relationshipName)) {
              // Search with relationshipName and propertyName
              $('#' + relationshipName + '-' + propertyName + '-secret_path').trigger('click');
            } else if (_.defined(capabilityName)) {
              // Search with capabilityName and propertyName
              $('#' + capabilityName + '-' + propertyName + '-secret_path').trigger('click');
            } else {
            	// Search only with propertyName
              $('#' + propertyName + '-secret_path').trigger('click');
            }
          }, 0);
        }

      });

    }
  ]); // controller
}); // define
