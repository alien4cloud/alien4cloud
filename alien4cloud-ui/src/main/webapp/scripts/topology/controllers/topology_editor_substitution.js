/**
*  Service that provides functionalities to edit nodes in a topology.
*/
define(function (require) {
  'use strict';

  var angular = require('angular');
  var modules = require('modules');

  modules.get('a4c-topology-editor').factory('topoEditSubstitution', [ 'topologyServices', 'suggestionServices',
    function(topologyServices, suggestionServices) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        /** Init method is called when the controller is ready. */
        init: function() {},

        refresh: function() {
          // about substitution
          if (this.scope.topology.topology.substitutionMapping && this.scope.topology.topology.substitutionMapping.substitutionType) {
            this.scope.substitutionType = this.scope.topology.topology.substitutionMapping.substitutionType.elementId;
            this.scope.substitutionVersion = this.scope.topology.topology.substitutionMapping.substitutionType.archiveVersion;
          } else {
            this.scope.substitutionType = undefined;
            this.scope.substitutionVersion = undefined;
          }
        },

        selectType: function(substitutionType) {
          var instance = this;
          if (!this.scope.topology.topology.substitutionMapping || this.scope.topology.topology.substitutionMapping.substitutionType !== substitutionType) {
            topologyServices.substitutionType.set({
              topologyId: instance.scope.topology.topology.id,
              elementId: substitutionType
            }, {}, function(result) {
              if (!result.error) {
                instance.scope.refreshTopology(result.data, instance.scope.selectedNodeTemplate ? instance.scope.selectedNodeTemplate.name : undefined);
              }
            });
          }
        },

        remove: function() {
          var instance = this;
          topologyServices.substitutionType.remove({
            topologyId: instance.scope.topology.topology.id
          }, {}, function(result) {
            if (!result.error) {
              instance.scope.refreshTopology(result.data, instance.scope.selectedNodeTemplate ? instance.scope.selectedNodeTemplate.name : undefined);
            }
          });
        },

        isTypeInDependencies: function(nodeType) {
          for (var i=0; i< this.scope.topology.topology.dependencies.length; i++) {
            if (this.scope.topology.topology.dependencies[i].name === nodeType.archiveName) {
              return true;
            }
          }
          return false;
        },

        exposeCapability: function(capabilityId) {
          var instance = this;
          if (this.isCapabilityExposed(capabilityId)) {
            return;
          }
          topologyServices.capabilitySubstitution.add({
            topologyId: instance.scope.topology.topology.id,
            nodeTemplateName: instance.scope.selectedNodeTemplate.name,
            substitutionCapabilityId: capabilityId,
            capabilityId: capabilityId
          }, {}, function(result) {
            if (!result.error) {
              instance.scope.refreshTopology(result.data, instance.scope.selectedNodeTemplate.name);
            }
          });
        },

        isCapabilityExposed: function(capabilityId) {
          var instance = this;
          var result = false;
          angular.forEach(instance.scope.topology.topology.substitutionMapping.capabilities, function(value) {
            if (value.nodeTemplateName === instance.scope.selectedNodeTemplate.name && value.targetId === capabilityId) {
              result = true;
            }
          });
          return result;
        },

        updateCababilityKey: function(oldKey, newKey) {
          var instance = this;
          topologyServices.capabilitySubstitution.update({
            topologyId: instance.scope.topology.topology.id,
            substitutionCapabilityId: oldKey,
            newCapabilityId: newKey
          }, {}, function(result) {
            if (!result.error) {
              instance.scope.refreshTopology(result.data, instance.scope.selectedNodeTemplate ? instance.scope.selectedNodeTemplate.name : undefined);
            }
          });
        },

        removeCabability: function(key) {
          var instance = this;
          topologyServices.capabilitySubstitution.remove({
            topologyId: instance.scope.topology.topology.id,
            substitutionCapabilityId: key
          }, {}, function(result) {
            if (!result.error) {
              instance.scope.refreshTopology(result.data, instance.scope.selectedNodeTemplate ? instance.scope.selectedNodeTemplate.name : undefined);
            }
          });
        },

        exposeRequirement: function(requirementId) {
          var instance = this;
          if (this.isRequirementExposed(requirementId)) {
            return;
          }
          topologyServices.requirementSubstitution.add({
            topologyId: instance.scope.topology.topology.id,
            nodeTemplateName: instance.scope.selectedNodeTemplate.name,
            substitutionRequirementId: requirementId,
            requirementId: requirementId
          }, {}, function(result) {
            if (!result.error) {
              instance.scope.refreshTopology(result.data, instance.scope.selectedNodeTemplate.name);
            }
          });
        },

        isRequirementExposed: function(requirementId) {
          var instance = this;
          var result = false;
          angular.forEach(instance.scope.topology.topology.substitutionMapping.requirements, function(value) {
            if (value.nodeTemplateName === instance.scope.selectedNodeTemplate.name && value.targetId === requirementId) {
              result = true;
            }
          });
          return result;
        },

        updateRequirementKey: function(oldKey, newKey) {
          var instance = this;
          topologyServices.requirementSubstitution.update({
            topologyId: instance.scope.topology.topology.id,
            substitutionRequirementId: oldKey,
            newRequirementId: newKey
          }, {}, function(result) {
            if (!result.error) {
              instance.scope.refreshTopology(result.data, instance.scope.selectedNodeTemplate ? instance.scope.selectedNodeTemplate.name : undefined);
            }
          });
        },

        removeRequirement: function(key) {
          var instance = this;
          topologyServices.requirementSubstitution.remove({
            topologyId: instance.scope.topology.topology.id,
            substitutionRequirementId: key
          }, {}, function(result) {
            if (!result.error) {
              instance.scope.refreshTopology(result.data, instance.scope.selectedNodeTemplate ? instance.scope.selectedNodeTemplate.name : undefined);
            }
          });
        },

        displayEmbededTopology: function(topologyId) {
          topologyServices.getTopologyVersion({
            topologyId: topologyId
          }, {}, function(result) {
            if (!result.error) {
              window.open('/#/topologytemplates/detail/' + result.data.topologyTemplateId + '/topology/' + result.data.version);
            }
          });
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.substitution = instance;
        scope.substitution.getTypeSuggestions = {
          get: suggestionServices.nodetypeSuggestions,
          waitBeforeRequest: 0, // TODO this seems unused...
          minLength: 2
        };
      };
    }
  ]); // modules
}); // define
