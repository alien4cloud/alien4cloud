define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/properties_services');

  modules.get('a4c-common').controller('SecretDisplay', ['$scope', '$translate', '$uibModal',
    function($scope, $translate, $uibModal) {
      if (_.undefined($scope.translated)) {
        $scope.translated = false;
      }

      $scope.secretSave = function(secretPath) {
        return $scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.SetPropertySecretOperation',
            nodeName: scope.selectedNodeTemplate.name,
            propertyName: propertyName,
            propertyValue: propertyValue
          },
          function(result){
            // successful callback
          },
          null,
          scope.selectedNodeTemplate.name,
          true
        );
      }
    }
  ]); // controller
}); // define
