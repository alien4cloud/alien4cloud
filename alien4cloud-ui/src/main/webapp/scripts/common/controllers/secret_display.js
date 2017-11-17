define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/properties_services');

  modules.get('a4c-common').controller('SecretDisplayCtrl', ['$scope', '$translate', '$uibModal',
    function($scope, $translate, $uibModal) {
      $scope.secretSave = function(secretPath) {
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
  ]); // controller
}); // define
