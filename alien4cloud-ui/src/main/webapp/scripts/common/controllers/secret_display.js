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

      $scope.secretSave = function(secretPath) {
        // Check the secretPath
        var error = check($scope, secretPath);
        if (_.defined(error)) {
          return error;
        }

        if (_.defined($scope.capabilityName)) {
          // It is the operation for capablity
          return $scope.execute()({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.SetNodeCapabilityPropertyAsSecretOperation',
              nodeName: $scope.selectedNodeTemplate.name,
              propertyName: $scope.propertyName,
              capabilityName: $scope.capabilityName,
              secretPath: secretPath
            },
            function(result){
              // successful callback
            },
            null,
            $scope.selectedNodeTemplate.name,
            true
          );
        } else {
          // It is an operation for property
          return $scope.execute()({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.SetNodePropertyAsSecretOperation',
              nodeName: $scope.selectedNodeTemplate.name,
              propertyName: $scope.propertyName,
              secretPath: secretPath
            },
            function(result){
              // successful callback
            },
            null,
            $scope.selectedNodeTemplate.name,
            true
          );
        }
      }
    }
  ]); // controller
}); // define
