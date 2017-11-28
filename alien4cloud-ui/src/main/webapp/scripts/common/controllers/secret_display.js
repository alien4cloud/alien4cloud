define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/properties_services');

  modules.get('a4c-common').controller('SecretDisplayCtrl', ['$scope', '$translate', '$uibModal',
    function($scope, $translate, $uibModal) {

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
        return $scope.onSave({secretPath: secretPath, propertyName: $scope.propertyName, propertyValue: $scope.propertyValue, capabilityName: $scope.capabilityName});
      };

      /*
      * A listener for focusing on the text editor.
      */
      $scope.$on('focus-on-' + $scope.propertyName, function(event) {
        var propertyName = event.currentScope.propertyName;
        setTimeout(function () {
          // Because the UI element is not yet loaded in the page.
          $('#p_secret_' + propertyName).trigger('click');
        }, 0);
      });

    }
  ]); // controller
}); // define
