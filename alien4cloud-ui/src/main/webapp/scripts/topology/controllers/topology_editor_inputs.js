/** Manage inputs in a topology edition. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor', ['pascalprecht.translate', 'toaster']).factory('topoEditInputs',
    ['$translate', 'toaster', '$alresource',
    function($translate, toaster, $alresource) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      var nodeInputResource = $alresource('rest/latest/editor/:topologyId/inputhelper/node');
      var capabilityPropInputResource = $alresource('rest/latest/editor/:topologyId/inputhelper/capability');
      var relationshipPropInputResource = $alresource('rest/latest/editor/:topologyId/inputhelper/relationship');

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,

        generateInputIdFromPropertyName: function(propertyName) {
          if (_.undefined(this.scope.topology.topology.inputs)) {
            return propertyName;
          }
          var i = 0;
          var inputId = propertyName;
          while (this.scope.topology.topology.inputs.hasOwnProperty(inputId)) {
            inputId = propertyName + '_' + i;
            i++;
          }
          return inputId;
        },

        createInput: function(inputId, propertyDefinition, callback) {
          this.scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation',
            inputName: inputId,
            propertyDefinition: propertyDefinition
          }, function(result) {
            if (!result.error) {
              callback();
            }
          });
        },

        createFromRelationshipProperty: function(relationshipName, propertyName) {
          var self = this;
          var selectedRelationshipType = this.scope.topology.relationshipTypes[this.scope.selectedNodeTemplate.relationshipsMap[relationshipName].value.type];
          var selectedRelationshipPropertyDefinition = selectedRelationshipType.propertiesMap[propertyName].value;
          var inputId = this.generateInputIdFromPropertyName(propertyName);
          this.createInput(inputId, selectedRelationshipPropertyDefinition, function() {
            self.scope.currentInputCandidatesForRelationshipProperty.push(inputId);
            self.toggleRelationshipProperty(relationshipName, propertyName, inputId);
          });
        },

        createFromCapabilityProperty: function(capabilityName, propertyName) {
          var self = this;
          var selectedCapabilityType = this.scope.topology.capabilityTypes[this.scope.selectedNodeTemplate.capabilitiesMap[capabilityName].value.type];
          var selectedCapabilityPropertyDefinition = selectedCapabilityType.propertiesMap[propertyName].value;
          var inputId = this.generateInputIdFromPropertyName(propertyName);
          this.createInput(inputId, selectedCapabilityPropertyDefinition, function() {
            self.scope.currentInputCandidatesForCapabilityProperty.push(inputId);
            self.toggleCapabilityProperty(capabilityName, propertyName, inputId);
          });
        },

        createFromProperty: function(propertyName) {
          var self = this;
          var selectedNodeTemplateType = this.scope.topology.nodeTypes[this.scope.selectedNodeTemplate.type];
          var selectedPropertyDefinition = selectedNodeTemplateType.propertiesMap[propertyName].value;
          var inputId = this.generateInputIdFromPropertyName(propertyName);
          this.createInput(inputId, selectedPropertyDefinition, function() {
            self.scope.currentInputCandidatesForProperty.push(inputId);
            self.toggleProperty(propertyName, inputId);
          });
        },

        getCandidatesForProperty: function(propertyName) {
          var self = this;
          this.scope.currentInputCandidatesForProperty = [];
          nodeInputResource.get({
            topologyId: self.scope.topology.topology.id,
            nodeTemplateName: self.scope.selectedNodeTemplate.name,
            propertyId: propertyName
          }, function(result) {
            self.scope.currentInputCandidatesForProperty = result.data;
          });
        },

        getCandidatesForCapabilityProperty: function(capabilityName, propertyName) {
          var self = this;
          this.scope.currentInputCandidatesForCapabilityProperty = [];
          capabilityPropInputResource.get({
            topologyId: self.scope.topology.topology.id,
            nodeTemplateName: self.scope.selectedNodeTemplate.name,
            propertyId: propertyName,
            capabilityId: capabilityName
          }, function(result) {
            self.scope.currentInputCandidatesForCapabilityProperty = result.data;
          });
        },

        getCandidatesForRelationshipProperty: function(relationshipName, propertyName) {
          var self = this;
          this.scope.currentInputCandidatesForRelationshipProperty = [];
          relationshipPropInputResource.get({
            topologyId: self.scope.topology.topology.id,
            nodeTemplateName: self.scope.selectedNodeTemplate.name,
            propertyId: propertyName,
            relationshipId: relationshipName
          }, function(result) {
            self.scope.currentInputCandidatesForRelationshipProperty = result.data;
          });
        },

        getCandidatesForArtifact: function(artifact) {
          // artifact filtering is based only on type, no need to reuse java code for constraints so process client side.
          var self = this;
          this.scope.currentInputCandidatesForArtifact = [];
          if(_.defined(this.scope.topology.topology.inputArtifacts)) {
            _.each(this.scope.topology.topology.inputArtifacts, function(inputArtifact, inputArtifactId) {
              if(artifact.artifactType === inputArtifact.artifactType) {
                self.scope.currentInputCandidatesForArtifact.push(inputArtifactId);
              }
            });
          }
        },

        toggleProperty: function(propertyName, inputId) {
          var scope = this.scope;
          if (!this.isPropertyAssociatedToInput(propertyName, inputId)) {
            this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodePropertyAsInputOperation',
              nodeName: scope.selectedNodeTemplate.name,
              propertyName: propertyName,
              inputName: inputId
            });
          } else {
            this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodePropertyAsInputOperation',
              nodeName: scope.selectedNodeTemplate.name,
              propertyName: propertyName
            });
          }
        },

        toggleRelationshipProperty: function(relationshipName, propertyName, inputId) {
          var scope = this.scope;
          if (!this.isRelationshipPropertyAssociatedToInput(relationshipName, propertyName, inputId)) {
            this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs.SetRelationshipPropertyAsInputOperation',
              nodeName: scope.selectedNodeTemplate.name,
              relationshipName: relationshipName,
              propertyName: propertyName,
              inputName: inputId
            });
          } else {
            this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs.UnsetRelationshipPropertyAsInputOperation',
              nodeName: scope.selectedNodeTemplate.name,
              relationshipName: relationshipName,
              propertyName: propertyName,
              inputName: inputId
            });
          }
        },

        toggleCapabilityProperty: function(capabilityName, propertyName, inputId) {
          var scope = this.scope;
          if (!this.isCapabilityPropertyAssociatedToInput(capabilityName, propertyName, inputId)) {
            this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeCapabilityPropertyAsInputOperation',
              nodeName: scope.selectedNodeTemplate.name,
              capabilityName: capabilityName,
              propertyName: propertyName,
              inputName: inputId
            });
          } else {
            this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodeCapabilityPropertyAsInputOperation',
              nodeName: scope.selectedNodeTemplate.name,
              capabilityName: capabilityName,
              propertyName: propertyName
            });
          }
        },

        // check if the artifact is associated with an input.
        isInputArtifact: function(artifactId) {
          var scope = this.scope;
          var artifactName = scope.selectedNodeTemplate.artifacts[artifactId].artifactName;
          var regex = /\{\s*get_input_artifact:\s*(\w+)\s*\}/;
          var result = regex.exec(artifactName);
          return result !== null && result.length > 0;
        },

        // check if the id of the artifact input is the given one.
        isArtifactAssociatedToInput: function(artifactId, inputArtifactId) {
          var scope = this.scope;
          var artifactName = scope.selectedNodeTemplate.artifacts[artifactId].artifactName;
          var regex = /\{\s*get_input_artifact:\s*(\w+)\s*\}/;
          var result = regex.exec(artifactName);
          return result !== null && result.length > 0 && result[1] === inputArtifactId;
        },

        isPropertyValueIsAssociatedToInput: function(propertyValue, inputId) {
          if (_.defined(propertyValue) && _.defined(propertyValue.parameters) && propertyValue.parameters.length > 0) {
            return propertyValue.parameters[0] === inputId;
          }
          return false;
        },

        isPropertyAssociatedToInput: function(propertyName, inputId) {
          var scope = this.scope;
          var propertyValue = scope.selectedNodeTemplate.propertiesMap[propertyName].value;
          return this.isPropertyValueIsAssociatedToInput(propertyValue, inputId);
        },

        isRelationshipPropertyAssociatedToInput: function(relationshipName, propertyName, inputId) {
          var scope = this.scope;
          var propertyValue = scope.selectedNodeTemplate.relationshipsMap[relationshipName].value.propertiesMap[propertyName].value;
          return this.isPropertyValueIsAssociatedToInput(propertyValue, inputId);
        },

        isCapabilityPropertyAssociatedToInput: function(capabilityName, propertyName, inputId) {
          var scope = this.scope;
          var propertyValue = scope.selectedNodeTemplate.capabilitiesMap[capabilityName].value.propertiesMap[propertyName].value;
          return this.isPropertyValueIsAssociatedToInput(propertyValue, inputId);
        },

        remove: function(inputId) {
          this.scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.inputs.DeleteInputOperation',
            inputName: inputId
          });
        },

        update: function(oldInput, newInput, inputDefinition) {
          var scope = this.scope;
          if (newInput === oldInput) {
            return;
          }
          this.scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.inputs.RenameInputOperation',
            nodeName: scope.selectedNodeTemplate.name,
            inputName: oldInput,
            newInputName: newInput
          },function(result){
            if(_.defined(result.error)){
              inputDefinition.inputId = oldInput;
              var msg = $translate.instant('ERRORS.' + result.error.code);
              toaster.pop('error', $translate.instant(msg), $translate.instant(msg), 6000, 'trustedHtml', null);
            }
          });
        },
        createFromArtifact: function(artifactId) {
          var scope = this.scope;
          scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeArtifactAsInputOperation',
            nodeName: scope.selectedNodeTemplate.name,
            inputName: artifactId,
            artifactName: artifactId
          });
        },
        toggleArtifact: function(artifactId, inputArtifactId) {
          var scope = this.scope;
          if (!this.isArtifactAssociatedToInput(artifactId, inputArtifactId)) {
            scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeArtifactAsInputOperation',
              nodeName: scope.selectedNodeTemplate.name,
              inputName: inputArtifactId,
              artifactName: artifactId
            });
          } else {
            scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodeArtifactAsInputOperation',
              nodeName: scope.selectedNodeTemplate.name,
              artifactName: artifactId
            });
          }
        },

        updateArtifactId: function(inputArtifactId, newId) {
          var scope = this.scope;
          if (inputArtifactId === newId) {
            return;
          }
          scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.inputs.RenameInputArtifactOperation',
            inputName: inputArtifactId,
            newInputName: newId
          });
        },

        removeArtifact: function(inputArtifactId) {
          var scope = this.scope;
          scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.inputs.DeleteInputArtifactOperation',
            inputName: inputArtifactId
          });
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.inputs = instance;
      };
    }
  ]); // modules
}); // define
