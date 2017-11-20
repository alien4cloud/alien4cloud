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
            if (property.value.parameters[0] !== "") {
              setTimeout(function () {
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
              }, 0);
            }
            // reset the secret to originalValue
            if (_.defined(property.value)) {
              property.value = property.originalValue;
              property.originalValue = undefined;
            }
          } else {
            property.originalValue = property.value;
            property.value = {function:'get_secret', parameters: ['']};
            // Trigger the editor to enter the secret
            setTimeout(function () {
              $('#p_secret_' + property.key).trigger('click');
            }, 0);

          }
        },
        toggleCapabilitySecret: function(property, capabilityName) {
          var scope = this.scope;
          if (scope.properties.isSecretValue(property.value)) {
            if (property.value.parameters[0] !== "") {
              setTimeout(function () {
                // Send the operation request to unset the capability
                scope.execute({
                    type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.UnsetNodeCapabilityPropertyAsSecretOperation',
                    nodeName: scope.selectedNodeTemplate.name,
                    propertyName: property.key,
                    capabilityName: capabilityName
                  },
                  function(result){
                    // successful callback
                  },
                  null,
                  scope.selectedNodeTemplate.name,
                  true
                );
              }, 0);
            }
            // reset the secret to originalValue
            if (_.defined(property.value)) {
              property.value = property.originalValue;
              property.originalValue = undefined;
            }

          } else {
            property.originalValue = property.value;
            property.value = {function:'get_secret', parameters: ['']};
            // Trigger the editor to enter the secret
            setTimeout(function () {
              $('#p_secret_' + property.key).trigger('click');
            }, 0);
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
