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
              type: 'org.alien4cloud.tosca.editor.operations.secrets.SetNodePropertyAsSecretOperation',
              nodeName: scope.selectedNodeTemplate.name,
              propertyName: propertyName,
              secretPath: secretPath
            },
            null,
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
              type: 'org.alien4cloud.tosca.editor.operations.secrets.SetNodeCapabilityPropertyAsSecretOperation',
              nodeName: scope.selectedNodeTemplate.name,
              propertyName: propertyName,
              capabilityName: capabilityName,
              secretPath: secretPath
            },
            null,
            null,
            scope.selectedNodeTemplate.name,
            true
          );
        },
        /*
        * It's a function for saving the relationship property as a secret.
        */
        saveNodeRelationshipPropertySecret: function(secretPath, propertyName, relationshipName) {
          var scope = this.scope;
          // It is the operation for capablity
          return scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.secrets.SetRelationshipPropertyAsSecretOperation',
              nodeName: scope.selectedNodeTemplate.name,
              propertyName: propertyName,
              relationshipName: relationshipName,
              secretPath: secretPath
            },
            null,
            null,
            scope.selectedNodeTemplate.name,
            true
          );
        },
        /*
        * It's a function for saving the policy property as a secret.
        */
        savePolicyPropertySecret: function(secretPath, propertyName) {
          var scope = this.scope;
          // It is the operation for capablity
          return scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.secrets.SetPolicyPropertyAsSecretOperation',
              policyName: scope.policies.selectedTemplate.name,
              propertyName: propertyName,
              secretPath: secretPath
            },
            null,
            null,
            scope.policies.selectedTemplate.name,
            true
          );
        },

        togglePropertySecret: function(property) {
          // This request object is for unset the property.
          var requestObject = {
              type: 'org.alien4cloud.tosca.editor.operations.secrets.UnsetNodePropertyAsSecretOperation',
              nodeName: this.scope.selectedNodeTemplate.name,
              propertyName: property.key
            };
          var broadcastObject = {
            'propertyName': property.key};
          this.toggleSecret(property, requestObject, broadcastObject);
        },

        toggleCapabilitySecret: function(property, capabilityName) {
          // This request object is for unset the capability.
          var requestObject = {
              type: 'org.alien4cloud.tosca.editor.operations.secrets.UnsetNodeCapabilityPropertyAsSecretOperation',
              nodeName: this.scope.selectedNodeTemplate.name,
              propertyName: property.key,
              capabilityName: capabilityName
            };
          var broadcastObject = {
            'propertyName': property.key,
            'capabilityName': capabilityName};
          this.toggleSecret(property, requestObject, broadcastObject);
        },

        toggleSecret: function(self, requestObject, broadcastObject) {
          var scope = this.scope;
          if (scope.properties.isSecretValue(self.value)) {
            self.value = null;
            scope.$root.$broadcast('reset-property', broadcastObject);
          } else {
            self.value = {function:'get_secret', parameters: ['']};
            // Trigger the editor to enter the secret
            scope.$root.$broadcast('focus-on', broadcastObject);
          }
        },


        /*
        * It's a function for policy secret button
          It receives a policyEntry like propertyEntry
        */
        togglePolicyPropertySecret: function(policy) {
          // This request object is for unset the property.
          var requestObject = {
              type: 'org.alien4cloud.tosca.editor.operations.secrets.UnsetPolicyPropertyAsSecretOperation',
              nodeName: this.scope.policies.selectedTemplate.name,
              propertyName: policy.key
            };
          var broadcastObject = {
            'propertyName': policy.key};
          this.toggleSecret(policy, requestObject, broadcastObject);
        },

        /*
        * It's a function only for custom operation
        */
        toggleInputParametersSecret: function(inputParameterName, inputParameter) {
          var scope = this.scope;
          // identifier to locate the button
          var broadcastObject = {
            'propertyName': inputParameterName};
          if (scope.properties.isSecretValue(inputParameter.paramValue)) {
            // reset the secret to originalValue
            inputParameter.paramValue = null;
            scope.$root.$broadcast('reset-property', broadcastObject);
          } else {
            inputParameter.paramValue = {function:'get_secret', parameters: ['']};
            // Trigger the editor to enter the secret
            scope.$root.$broadcast('focus-on', broadcastObject);
          }
        },

        /*
        *It's a function for managing property secret in the location resource
        */
        toggleResourcePropertySecret: function(self) {
          var scope = this.scope;
          var broadcastObject = {
            'propertyName': self.key};
          if (scope.properties.isSecretValue(self.value)) {
            // Unset the property
            self.value = null;
            scope.$root.$broadcast('reset-property', broadcastObject);
          } else {
            self.value = {function:'get_secret', parameters: ['']};
            // Trigger the editor to enter the secret
            scope.$root.$broadcast('focus-on', broadcastObject);
          }
        },

        /*
        *It's a function for managing property secret in the location resource
        */
        toggleResourceCapabilitySecret: function(capabilityName, property) {
          var scope = this.scope;
          var broadcastObject = {
            'capabilityName': capabilityName,
            'propertyName': property.key};
          if (scope.properties.isSecretValue(property.value)) {
            // Unset the property
            property.value = null;
            scope.$root.$broadcast('reset-property', broadcastObject);
          } else {
            property.value = {function:'get_secret', parameters: ['']};
            // Trigger the editor to enter the secret
            scope.$root.$broadcast('focus-on', broadcastObject);
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
          var broadcastObject = {
            'propertyName': inputId};
          if (scope.properties.isSecretValue(property)) {
            // Unset the property
            property = null;
            scope.$root.$broadcast('reset-property', broadcastObject);
          } else {
            _.forEach(_.keys(property), function(key){
              delete property[key];
            });
            property['function'] = 'get_secret';
            property['parameters'] = [''];
            // Trigger the editor to enter the secret
            scope.$root.$broadcast('focus-on', broadcastObject);
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
          var broadcastObject = {
            'propertyName': inputId};
          if (scope.properties.isSecretValue(property)) {
            // Unset the property
            property = null;
            scope.$root.$broadcast('reset-property', broadcastObject);
          } else {
            _.forEach(_.keys(property), function(key){
              delete property[key];
            });
            property['function'] = 'get_secret';
            property['parameters'] = [''];
            // Trigger the editor to enter the secret
            scope.$root.$broadcast('focus-on', broadcastObject);
          }
        },

        toggleRelationshipPropertySecret: function(property, relationshipName) {
          var scope = this.scope;
          // This request object is for unset the property.
          var requestObject = {
              type: 'org.alien4cloud.tosca.editor.operations.secrets.UnsetRelationshipPropertyAsSecretOperation',
              nodeName: this.scope.selectedNodeTemplate.name,
              propertyName: property.key,
              relationshipName: relationshipName
            };
          // Broadcast an event to auto open the editor inside the secret display.
          var broadcastObject = {
            'propertyName': property.key,
            'relationshipName': relationshipName};
          this.toggleSecret(property, requestObject, broadcastObject);
        }


      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.secrets = instance;
      };
    }
  ]); // modules
}); // define
