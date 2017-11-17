/**
*  Service that provides functionalities to edit nodes secret in a topology.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/controllers/confirm_modal');

  modules.get('a4c-topology-editor').factory('topoEditSecrets', [
    function() {

      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        togglePropertySecret: function(propertyName, propertyValue) {
          // valid for the property
          var scope = this.scope;
          if (scope.properties.isSecretValue(propertyValue)) {
            // reset the secret to originalValue
            if (_.defined(propertyValue) && _.defined(propertyValue.originalValue)) {
              propertyValue.value = propertyValue.originalValue;
              propertyValue.originalValue = undefined;
            }
            scope.topology.nodeTypes[scope.selectedNodeTemplate.type].propertiesMap[propertyName].value.secret = false;
          } else {
            // set the secret
            if (_.defined(propertyValue)) {
              propertyValue.value = propertyValue.originalValue;
            }
            scope.topology.nodeTypes[scope.selectedNodeTemplate.type].properties[propertyName] = {function:'get_secret', parameters: ['']};
            scope.topology.nodeTypes[scope.selectedNodeTemplate.type].propertiesMap[propertyName].value.secret = true;
          }
        },
        toggleCapabilityPropertySecret: function(propertyName, propertyValue) {
          // valid for the property
          var scope = this.scope;
          if (scope.properties.isSecretValue(propertyValue)) {
            // reset the secret to originalValue
            if (_.defined(propertyValue) && _.defined(propertyValue.originalValue)) {
              propertyValue.value = propertyValue.originalValue;
              propertyValue.originalValue = undefined;
            }
            // scope.topology.nodeTypes[scope.selectedNodeTemplate.type].propertiesMap[propertyName].value.secret = false;
          } else {
            // set the secret
            if (_.defined(propertyValue)) {
              propertyValue.value = propertyValue.originalValue;
            }
            scope.topology.nodeTypes[scope.selectedNodeTemplate.type].capabilities[propertyName] = {function:'get_secret', parameters: ['']};
            // scope.topology.nodeTypes[scope.selectedNodeTemplate.type].propertiesMap[propertyName].value.secret = true;
          }
        }

      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.secrets = instance;
      };
    }
  ]); // modules
}); // define
