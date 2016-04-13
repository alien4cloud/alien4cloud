/** Manage inputs in a topology edition. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  modules.get('a4c-topology-editor', ['pascalprecht.translate', 'toaster']).factory('topoEditInputs',
    ['topologyServices', '$translate', 'toaster',
    function(topologyServices, $translate, toaster) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

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
          var instance = this;
          topologyServices.inputs.add({
            topologyId: instance.scope.topology.topology.id,
            inputId: inputId
          }, angular.toJson(propertyDefinition), function(success) {
            if (!success.error) {
              instance.scope.refreshTopology(success.data);
              callback();
            }
          });
        },

        createFromRelationshipProperty: function(relationshipName, propertyName) {
          var instance = this;
          var selectedRelationshipType = this.scope.topology.relationshipTypes[this.scope.selectedNodeTemplate.relationshipsMap[relationshipName].value.type];
          var selectedRelationshipPropertyDefinition = selectedRelationshipType.propertiesMap[propertyName].value;
          var inputId = this.generateInputIdFromPropertyName(propertyName);
          this.createInput(inputId, selectedRelationshipPropertyDefinition, function() {
            instance.scope.currentInputCandidatesForRelationshipProperty.push(inputId);
            instance.toggleRelationshipProperty(relationshipName, propertyName, inputId);
          });
        },

        createFromCapabilityProperty: function(capabilityName, propertyName) {
          var instance = this;
          var selectedCapabilityType = this.scope.topology.capabilityTypes[this.scope.selectedNodeTemplate.capabilitiesMap[capabilityName].value.type];
          var selectedCapabilityPropertyDefinition = selectedCapabilityType.propertiesMap[propertyName].value;
          var inputId = this.generateInputIdFromPropertyName(propertyName);
          this.createInput(inputId, selectedCapabilityPropertyDefinition, function() {
            instance.scope.currentInputCandidatesForCapabilityProperty.push(inputId);
            instance.toggleCapabilityProperty(capabilityName, propertyName, inputId);
          });
        },

        createFromProperty: function(propertyName) {
          var instance = this;
          var selectedNodeTemplateType = this.scope.topology.nodeTypes[this.scope.selectedNodeTemplate.type];
          var selectedPropertyDefinition = selectedNodeTemplateType.propertiesMap[propertyName].value;
          var inputId = this.generateInputIdFromPropertyName(propertyName);
          this.createInput(inputId, selectedPropertyDefinition, function() {
            instance.scope.currentInputCandidatesForProperty.push(inputId);
            instance.toggleProperty(propertyName, inputId);
          });
        },

        createFromArtifact: function(artifactId) {
          var instance = this;
          topologyServices.nodeTemplate.artifacts.setInput({
            topologyId: instance.scope.topology.topology.id,
            nodeTemplateName: instance.scope.selectedNodeTemplate.name,
            artifactId: artifactId,
            inputArtifactId: artifactId
          }, {}, function(result) {
            if (!result.error) {
              instance.scope.refreshTopology(result.data);
            }
          });
        },

        getCandidatesForProperty: function(propertyName) {
          var instance = this;
          this.scope.currentInputCandidatesForProperty = [];
          topologyServices.nodeTemplate.getInputCandidates.getCandidates({
            topologyId: instance.scope.topology.topology.id,
            nodeTemplateName: instance.scope.selectedNodeTemplate.name,
            propertyId: propertyName
          }, function(success) {
            instance.scope.currentInputCandidatesForProperty = success.data;
          });
        },

        getCandidatesForArtifact: function(artifactId) {
          var instance = this;
          this.scope.currentInputCandidatesForArtifact = [];
          topologyServices.nodeTemplate.artifacts.getInputCandidates({
            topologyId: instance.scope.topology.topology.id,
            nodeTemplateName: instance.scope.selectedNodeTemplate.name,
            artifactId: artifactId
          }, function(success) {
            instance.scope.currentInputCandidatesForArtifact = success.data;
          });
        },

        getCandidatesForRelationshipProperty: function(relationshipName, propertyName) {
          var instance = this;
          this.scope.currentInputCandidatesForRelationshipProperty = [];
          topologyServices.nodeTemplate.relationship.getInputCandidates.getCandidates({
            topologyId: instance.scope.topology.topology.id,
            nodeTemplateName: instance.scope.selectedNodeTemplate.name,
            propertyId: propertyName,
            relationshipId: relationshipName
          }, function(success) {
            instance.scope.currentInputCandidatesForRelationshipProperty = success.data;
          });
        },

        getCandidatesForCapabilityProperty: function(capabilityName, propertyName) {
          var instance = this;
          this.scope.currentInputCandidatesForCapabilityProperty = [];
          topologyServices.nodeTemplate.capability.getInputCandidates.getCandidates({
            topologyId: instance.scope.topology.topology.id,
            nodeTemplateName: instance.scope.selectedNodeTemplate.name,
            propertyId: propertyName,
            capabilityId: capabilityName
          }, function(success) {
            instance.scope.currentInputCandidatesForCapabilityProperty = success.data;
          });
        },

        toggleProperty: function(propertyName, inputId) {
          var scope = this.scope;
          if (!this.isPropertyAssociatedToInput(propertyName, inputId)) {
            topologyServices.nodeTemplate.setInputs.set({
              topologyId: scope.topology.topology.id,
              inputId: inputId,
              nodeTemplateName: scope.selectedNodeTemplate.name,
              propertyId: propertyName
            }, function(success) {
              if (!success.error) {
                scope.refreshTopology(success.data);
              }
            });
          } else {
            topologyServices.nodeTemplate.setInputs.unset({
              topologyId: scope.topology.topology.id,
              nodeTemplateName: scope.selectedNodeTemplate.name,
              propertyId: propertyName
            }, function(success) {
              if (!success.error) {
                scope.refreshTopology(success.data);
              }
            });
          }
        },

        toggleArtifact: function(artifactId, inputArtifactId) {
          var scope = this.scope;
          if (!this.isArtifactAssociatedToInput(artifactId, inputArtifactId)) {
            topologyServices.nodeTemplate.artifacts.setInput({
              topologyId: scope.topology.topology.id,
              nodeTemplateName: scope.selectedNodeTemplate.name,
              artifactId: artifactId,
              inputArtifactId: inputArtifactId
            }, {}, function(result) {
              if (!result.error) {
                scope.refreshTopology(result.data);
              }
            });
          } else {
            topologyServices.nodeTemplate.artifacts.unsetInput({
              topologyId: scope.topology.topology.id,
              nodeTemplateName: scope.selectedNodeTemplate.name,
              artifactId: artifactId,
              inputArtifactId: inputArtifactId
            }, {}, function(result) {
              if (!result.error) {
                scope.refreshTopology(result.data);
              }
            });
          }
        },

        updateArtifactId: function(inputArtifactId, newId) {
          var scope = this.scope;
          if (inputArtifactId === newId) {
            return;
          }
          topologyServices.inputArtifacts.rename({
            topologyId: scope.topology.topology.id,
            inputArtifactId: inputArtifactId
          }, {newId: newId}, function(result) {
            if (!result.error) {
              scope.refreshTopology(result.data);
            }
          });
        },

        removeArtifact: function(inputArtifactId) {
          var scope = this.scope;
          topologyServices.inputArtifacts.remove({
            topologyId: scope.topology.topology.id,
            inputArtifactId: inputArtifactId
          }, {}, function(result) {
            if (!result.error) {
              scope.refreshTopology(result.data);
            }
          });
        },

        toggleRelationshipProperty: function(relationshipName, propertyName, inputId) {
          var scope = this.scope;
          if (!this.isRelationshipPropertyAssociatedToInput(relationshipName, propertyName, inputId)) {
            topologyServices.nodeTemplate.relationship.setInputs.set({
              topologyId: scope.topology.topology.id,
              inputId: inputId,
              nodeTemplateName: scope.selectedNodeTemplate.name,
              propertyId: propertyName,
              relationshipId: relationshipName
            }, function(success) {
              if (!success.error) {
                scope.refreshTopology(success.data);
              }
            });
          } else {
            topologyServices.nodeTemplate.relationship.setInputs.unset({
              topologyId: scope.topology.topology.id,
              nodeTemplateName: scope.selectedNodeTemplate.name,
              propertyId: propertyName,
              relationshipId: relationshipName
            }, function(success) {
              if (!success.error) {
                scope.refreshTopology(success.data);
              }
            });
          }
        },

        toggleCapabilityProperty: function(capabilityName, propertyName, inputId) {
          var scope = this.scope;
          if (!this.isCapabilityPropertyAssociatedToInput(capabilityName, propertyName, inputId)) {
            topologyServices.nodeTemplate.capability.setInputs.set({
              topologyId: scope.topology.topology.id,
              inputId: inputId,
              nodeTemplateName: scope.selectedNodeTemplate.name,
              propertyId: propertyName,
              capabilityId: capabilityName
            }, function(success) {
              if (!success.error) {
                scope.refreshTopology(success.data);
              }
            });
          } else {
            topologyServices.nodeTemplate.capability.setInputs.unset({
              topologyId: scope.topology.topology.id,
              nodeTemplateName: scope.selectedNodeTemplate.name,
              propertyId: propertyName,
              capabilityId: capabilityName
            }, function(success) {
              if (!success.error) {
                scope.refreshTopology(success.data);
              }
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
          var scope = this.scope;
          topologyServices.inputs.remove({
            topologyId: scope.topology.topology.id,
            inputId: inputId
          }, function(success) {
            if (!success.error) {
              scope.refreshTopology(success.data);
            }
          });
        },

        update: function(oldInput, newInput, inputDefinition) {
          var scope = this.scope;
          if (newInput === oldInput) {
            return;
          }
          topologyServices.inputs.update({
            topologyId: scope.topology.topology.id,
            inputId: oldInput,
            newInputId: newInput
          }, function(success) {
            if (!success.error) {
              scope.refreshTopology(success.data);
            } else {
              inputDefinition.inputId = oldInput;
              var msg = $translate('ERRORS.' + success.error.code);
              toaster.pop('error', $translate(msg), $translate(msg), 6000, 'trustedHtml', null);
            }
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
