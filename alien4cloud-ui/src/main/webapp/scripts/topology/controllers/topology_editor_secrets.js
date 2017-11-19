/**
*  Service that provides functionalities to edit nodes secret in a topology.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('topoEditSecrets', [
    function() {

      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        init: function() {},
        togglePropertySecret: function(property) {
          var scope = this.scope;
          if (scope.properties.isSecretValue(property.value)) {
            // reset the secret to originalValue
            if (_.defined(property.value)) {
              property.value = property.originalValue;
              property.originalValue = undefined;
            }
            // Send the operation request to unset the property
            scope.execute({
                type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.UnsetNodePropertyAsSecretOperation',
                nodeName: scope.selectedNodeTemplate.name,
                propertyName: property.key
              },
              function(result){
                // successful callback
              },
              null,
              scope.selectedNodeTemplate.name,
              true
            );
          } else {
            property.originalValue = property.value;
            property.value = {function:'get_secret', parameters: ['']};
          }
        },
        toggleCapabilitySecret: function(capability) {
          var scope = this.scope;
          if (scope.properties.isSecretValue(capability.value)) {
            // reset the secret to originalValue
            if (_.defined(capability.value)) {
              capability.value = capability.originalValue;
              capability.originalValue = undefined;
            }
            // Send the operation request to unset the property
            scope.execute({
                type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.UnsetNodeCapabilityPropertyAsSecretOperation',
                nodeName: scope.selectedNodeTemplate.name,
                propertyName: "",
                capabilityName: capability.key
              },
              function(result){
                // successful callback
              },
              null,
              scope.selectedNodeTemplate.name,
              true
            );
          } else {
            capability.originalValue = capability.value;
            capability.value = {function:'get_secret', parameters: ['']};
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
