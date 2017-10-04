/**
*  Service that provides functionalities to edit nodes in a topology.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/common/controllers/confirm_modal');

  modules.get('a4c-topology-editor').factory('topoEditPolicies', ['toscaService', '$filter', '$uibModal', '$translate',
    function(toscaService, $filter, $uibModal, $translate) {
      var policyNamePattern = '^\\w+$';

      var TopologyEditorMixin = function(scope) {
        var self = this;
        this.scope = scope;
        this.scope.$on('topologyRefreshedEvent', function() {
          _.each(self.scope.topology.topology.policies, function(policyTemplate) {
            if(_.defined(self.selectedTemplate)) {
              policyTemplate.selected = self.selectedTemplate.name === policyTemplate.name;
              if(policyTemplate.selected) {
                self.doSelectPolicy(policyTemplate);
              }
            }
          });
        });
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        edit: {
          name: '',
          targetSuggestions: []
        },
        selectPolicy: function(policyTemplate) {
          if(_.defined(this.selectedTemplate)) {
            this.selectedTemplate.selected = false;
          }
          if(_.defined(this.selectedTemplate) && this.selectedTemplate.name === policyTemplate.name) {
            this.selectedTemplate = undefined;
          } else {
            // compute target suggestions for this policy template (all node template names but current selected targets)
            this.doSelectPolicy(policyTemplate);
          }
        },
        doSelectPolicy: function(policyTemplate) {
          this.selectedTemplate = policyTemplate;
          this.selectedTemplate.selected = true;

          var self = this;
          this.edit.targetSuggestions = [];
          _.each(this.scope.topology.topology.nodeTemplates, function(nodeTemplate) {
            if(!_.contains(self.selectedTemplate.targets, nodeTemplate.name)) {
              self.edit.targetSuggestions.push(nodeTemplate.name);
            }
          });

          this.edit.name = policyTemplate.name;
          this.scope.$broadcast('editorSelectionChangedEvent', { nodeNames: this.selectedTemplate.targets });
        },
        isSelected: function(policyTemplate) {
          return policyTemplate.name === this.selectedTemplate.name;
        },
        /** Method triggered as a result of a on-drag (see drag and drop directive and node type search directive). */
        onDragged: function(e) {
          var policyType = angular.fromJson(e.source);
          this.add(policyType);
        },
        /** this has to be exposed to the scope as we cannot rely on drag and drop callbacks for ui tests */
        add: function(policyType) {
          var self = this;
          var policyTemplateName = toscaService.generateTemplateName(policyType.elementId, this.scope.topology.topology.policies);
          // Add policy operation automatically change dependency version to higher so if different warn the user.
          var currentVersion = this.getDepVersionIfDifferent(policyType.archiveName, policyType.archiveVersion, this.scope.topology.topology.dependencies);
          if(_.defined(currentVersion)) {
            var modalInstance = $uibModal.open({
              templateUrl: 'views/common/confirm_modal.html',
              controller: 'ConfirmModalCtrl',
              resolve: {
                title: function() {
                  return 'APPLICATIONS.TOPOLOGY.DEPENDENCIES.VERSION_CONFLICT_TITLE';
                },
                content: function() {
                  return $translate('APPLICATIONS.TOPOLOGY.DEPENDENCIES.VERSION_CONFLICT_MSG', {
                    name: policyType.archiveName,
                    current: currentVersion,
                    new: policyType.archiveVersion
                  });
                }
              }
            });
            modalInstance.result.then(function () {
              self.doAddPolicyTemplate(policyTemplateName, policyType);
            });
          } else {
            this.doAddPolicyTemplate(policyTemplateName, policyType);
          }
        },
        /** Actually trigger the node template addition. */
        doAddPolicyTemplate: function(policyTemplateName, policyType) {
          var scope = this.scope;
          // Add node operation automatically change dependency version to higher so if different warn the user.
          scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.policies.AddPolicyOperation',
            policyName: policyTemplateName,
            policyTypeId: policyType.id
          });
        },
        getDepVersionIfDifferent: function(archiveName, archiveVersion, dependencies) {
          if(_.undefined(dependencies)) {
            return null;
          }
          for(var i=0; i< dependencies.length; i++) {
            if(dependencies[i].name === archiveName) {
              if(dependencies[i].version === archiveVersion) {
                return null;
              }
              return dependencies[i].version;
            }
          }
          return null;
        },
        /* Update node template name */
        updateName: function(newName) {
          var self = this;

          if (!newName.match(policyNamePattern)) {
            return $filter('translate')('APPLICATIONS.TOPOLOGY.INVALID_NAME');
          }

          if (this.selectedTemplate.name !== newName) {
            this.scope.execute({
                type: 'org.alien4cloud.tosca.editor.operations.policies.RenamePolicyOperation',
                policyName: this.selectedTemplate.name,
                newName: newName
              }, null,
              function() { // error handling
                self.edit.name = this.selectedTemplate.name;
              });
          } // if end
        },
        delete: function(policyTemplateName) {
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.policies.DeletePolicyOperation',
              policyName: policyTemplateName
            });
        },
        addTarget: function(newTarget) {
          var targets = [];
          _.each(this.selectedTemplate.targets, function(target) { targets.push(target); });
          targets.push(newTarget);
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyTargetsOperation',
              policyName: this.selectedTemplate.name,
              targets: targets
            });
        },
        removeTarget: function(removedTarget) {
          var targets = [];
          _.each(this.selectedTemplate.targets, function(target) {
            if(removedTarget !== target) {
              targets.push(target);
            }
          });
          this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyTargetsOperation',
              policyName: this.selectedTemplate.name,
              targets: targets
            });
        },
        /* Update properties of a node template */
        updateProperty: function(propertyName, propertyValue) {
          var self = this;

          return this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyPropertyValueOperation',
              policyName: this.selectedTemplate.name,
              propertyName: propertyName,
              propertyValue: propertyValue
            },
            function(result){
              if (_.undefined(result.error)) {
                self.selectedTemplate.propertiesMap[propertyName].value = {value: propertyValue, definition: false};
              }
            }
          );
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.policies = instance;
      };
    }
  ]); // modules
}); // define
