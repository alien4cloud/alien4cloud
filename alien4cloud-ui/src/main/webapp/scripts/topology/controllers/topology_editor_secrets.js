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

        /*
        * It's a function for saving the node property as a secret.
        */
        saveNodePropertySecret: function(secretPath, propertyName) {
          var scope = this.scope;
          // It is an operation for property
          return scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.SetNodePropertyAsSecretOperation',
              nodeName: scope.selectedNodeTemplate.name,
              propertyName: propertyName,
              secretPath: secretPath
            },
            function(result){
              // successful callback
            },
            null,
            scope.selectedNodeTemplate.name,
            true
          );
        },
        /*
        * It's a function for saving the node capablity property as a secret.
        */
        saveNodeCapabilityPropertySecret: function(secretPath, propertyName, capabilityName) {
          var scope = this.scope;
          // It is the operation for capablity
          return scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.SetNodeCapabilityPropertyAsSecretOperation',
              nodeName: scope.selectedNodeTemplate.name,
              propertyName: propertyName,
              capabilityName: capabilityName,
              secretPath: secretPath
            },
            function(result){
              // successful callback
            },
            null,
            scope.selectedNodeTemplate.name,
            true
          );
        },
        /*
        * It's a function for saving the relationship property as a secret.
        */
        saveNodeRelationshipPropertySecret: function(secretPath) {
          var scope = this.scope;
          // Check the secretPath
          var error = check(scope, secretPath);
          if (_.defined(error)) {
            return error;
          }
          // It is the operation for capablity
          return scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.SetNodeRelationshipPropertyAsSecretOperation',
              nodeName: scope.selectedNodeTemplate.name,
              propertyName: scope.propertyName,
              capabilityName: scope.capabilityName,
              secretPath: secretPath
            },
            function(result){
              // successful callback
            },
            null,
            scope.selectedNodeTemplate.name,
            true
          );
        },

        togglePropertySecret: function(property) {
          var requestObject = {
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.UnsetNodePropertyAsSecretOperation',
              nodeName: this.scope.selectedNodeTemplate.name,
              propertyName: property.key
            };
          this.toggleSecret(property, requestObject);
        },

        toggleCapabilitySecret: function(property, capabilityName) {
          var requestObject = {
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.secrets.UnsetNodeCapabilityPropertyAsSecretOperation',
              nodeName: this.scope.selectedNodeTemplate.name,
              propertyName: property.key,
              capabilityName: capabilityName
            };
          this.toggleSecret(property, requestObject);
        },

        toggleSecret: function(self, requestObject) {
          var scope = this.scope;
          if (scope.properties.isSecretValue(self.value)) {
            if (self.value.parameters[0] !== "") {
              setTimeout(function () {
                scope.execute(requestObject,
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
            self.value = null;
            //Send an event to reset???
            //$rootScope.$broadcast('reset-property-' + self.key);
            // setTimeout(function () {
            //   $('#reset-property-' + self.key).trigger('click');
            // }, 0);
          } else {
            self.value = {function:'get_secret', parameters: ['']};
            // Trigger the editor to enter the secret
            setTimeout(function () {
              $('#p_secret_' + self.key).trigger('click');
            }, 0);
          }
        },

        // It's a function only for custom operation
        toggleInputSecret: function(inputParameterName, inputParameter) {
          var scope = this.scope;
          if (scope.properties.isSecretValue(inputParameter.paramValue)) {
            // reset the secret to originalValue
            inputParameter.paramValue = null;
            setTimeout(function () {
              $('#reset-property-' + inputParameterName).trigger('click');
            }, 0);
          } else {
            inputParameter.paramValue = {function:'get_secret', parameters: ['']};
            // Trigger the editor to enter the secret
            setTimeout(function () {
              $('#p_secret_' + inputParameterName).trigger('click');
            }, 0);
          }
        },

        //It's a function for managing property secret in the location resource
        toggleResourcePropertySecret: function(self) {
          var scope = this.scope;
          if (scope.properties.isSecretValue(self.value)) {
            // Unset the property
            self.value = null;
            setTimeout(function () {
              $('#reset-property-' + self.key).trigger('click');
            }, 0);
          } else {
            self.value = {function:'get_secret', parameters: ['']};
            // Trigger the editor to enter the secret
            setTimeout(function () {
              $('#p_secret_' + self.key).trigger('click');
            }, 0);
          }
        },

        /*
        * It's a function for toggling input secret.
          Here we consider the value as a reference.
        */
        toggleInputSecret: function(topology, inputId) {
          if (_.undefined(topology.deployerInputProperties[inputId])) {
            topology.deployerInputProperties[inputId] = {};
          }
          var property = topology.deployerInputProperties[inputId];
          var scope = this.scope;
          if (scope.properties.isSecretValue(property)) {
            // Unset the property
            property = null;
            setTimeout(function () {
              $('#reset-property-' + inputId).trigger('click');
            }, 0);
          } else {
            _.forEach(_.keys(property), function(key){
              delete property[key];
            });
            property['function'] = 'get_secret';
            property['parameters'] = [''];
            // Trigger the editor to enter the secret
            setTimeout(function () {
              $('#p_secret_' + inputId).trigger('click');
            }, 0);
          }
        },

        /*
        * It's a function for toggling preconfigured input secret.
          Here we consider the value as a reference.
        */
        togglePreconfiguredInputSecret: function(topology, inputId) {
          if (_.undefined(topology.preconfiguredInputProperties[inputId])) {
            topology.preconfiguredInputProperties[inputId] = {};
          }
          var property = topology.preconfiguredInputProperties[inputId];
          var scope = this.scope;
          if (scope.properties.isSecretValue(property)) {
            // Unset the property
            property = null;
            setTimeout(function () {
              $('#reset-property-' + inputId).trigger('click');
            }, 0);
          } else {
            _.forEach(_.keys(property), function(key){
              delete property[key];
            });
            property['function'] = 'get_secret';
            property['parameters'] = [''];
            // Trigger the editor to enter the secret
            setTimeout(function () {
              $('#p_secret_' + inputId).trigger('click');
            }, 0);
          }
        },

        toggleRelationshipPropertySecret: function(property) {
          var scope = this.scope;
          if (scope.properties.isSecretValue(property.value)) {
            // Unset the property
            property.value = null;
            setTimeout(function () {
              $('#reset-property-' + property.key).trigger('click');
            }, 0);
          } else {
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
