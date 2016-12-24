/**
*  Topology editor display controller. This service is responsible for augmenting the editor scope to manage elements that should be displayed and resize.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('topoEditRelationships', ['$modal', 'nodeTemplateService',
    function($modal, nodeTemplateService) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        /** Init method is called when the controller is ready. */
        init: function() {
          this.scope.relNameObj = {};
        },

        // do effectively add the relationship
        doAddRelationship: function(nodeTemplateName, relationshipInfo, requirementName, requirementType) {
          var scope = this.scope;
          scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation',
            nodeName: nodeTemplateName,
            relationshipName: relationshipInfo.name,
            relationshipType: relationshipInfo.relationship.elementId,
            relationshipVersion: relationshipInfo.relationship.archiveVersion,
            requirementName: requirementName,
            requirementType: requirementType,
            target: relationshipInfo.target,
            targetedCapabilityName: relationshipInfo.targetedCapabilityName
          }, null, null, nodeTemplateName);
          scope.display.set('nodetemplate', true);
        },

        openSearchRelationshipModal: function(sourceNodeTemplateName, requirementName, targetNodeTemplateName,
          targetedCapability, previousRelationshipName) {
          var scope = this.scope;
          var instance = this;

          var sourceNodeTemplate = scope.topology.topology.nodeTemplates[sourceNodeTemplateName];
          var requirement = sourceNodeTemplate.requirementsMap[requirementName].value;
          instance.createNewRelationship = previousRelationshipName ? false : true;

          if (instance.createNewRelationship && !requirement.canAddRel.yes) {
            // TODO we must display an error message...
            return;
          }

          scope.sourceElement = sourceNodeTemplate;
          scope.sourceElementName = sourceNodeTemplateName;
          scope.requirementName = requirementName;
          scope.requirement = requirement;
          scope.targetNodeTemplateName = targetNodeTemplateName;
          scope.targetedCapability = targetedCapability;

          var modalInstance = $modal.open({
            templateUrl: 'views/topology/search_relationship_modal.html',
            controller: 'SearchRelationshipCtrl',
            windowClass: 'searchModal',
            scope: scope,
            resolve: {
              useProvidedDefault: function() { return instance.createNewRelationship; }
            }
          });

          modalInstance.result.then(function(relationshipResult) {
            if (instance.createNewRelationship) {
              instance.doAddRelationship(sourceNodeTemplateName, relationshipResult, requirementName, requirement.type);
              return;
            }
            // This is a relationship change - remove the previous relationship then add the new one.
            // FIXME: If a relationship with the same name than the new one exists, the change fails (OK) and the previous relationship is lost
            instance.remove(previousRelationshipName, sourceNodeTemplate, function (result) {
              instance.doAddRelationship(sourceNodeTemplateName, relationshipResult, requirementName, requirement.type);
            });
          });
        },

        autoOpenRelationshipModal: function(sourceNodeTemplateName, targetNodeTemplateName) {
          var scope = this.scope;
          var sourceNodeTemplate = scope.topology.topology.nodeTemplates[sourceNodeTemplateName];
          var targetNodeTemplate = scope.topology.topology.nodeTemplates[targetNodeTemplateName];
          if (_.defined(sourceNodeTemplate) && _.defined(targetNodeTemplate)) {
            // let's try to find the requirement / for now we just support hosted on but we should improve that...
            var requirementName = nodeTemplateService.getContainerRequirement(sourceNodeTemplate, scope.topology.nodeTypes, scope.topology.relationshipTypes, scope.topology.capabilityTypes);
            this.openSearchRelationshipModal(sourceNodeTemplateName, requirementName, targetNodeTemplateName);
          }
        },

        updateRelationshipName: function(oldName, newName) {
          var scope = this.scope;
          // Update only when the name has changed
          if (oldName !== newName) {
            scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.relationshiptemplate.RenameRelationshipOperation',
              nodeName: scope.selectedNodeTemplate.name,
              relationshipName: oldName,
              newRelationshipName: newName
            }, function(){
              delete scope.relNameObj[oldName];
            }, function(){
              scope.relNameObj[oldName] = oldName;
            });
          }
        },

        updateRelationshipProperty: function(propertyDefinition, propertyName, propertyValue, relationshipType, relationshipName) {
          var scope = this.scope;
          return scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.relationshiptemplate.UpdateRelationshipPropertyValueOperation',
              nodeName: scope.selectedNodeTemplate.name,
              relationshipName: relationshipName,
              propertyName: propertyName,
              propertyValue: propertyValue
            },
            function(result) {
              if (_.undefined(result.error)) {
                scope.topology.topology.nodeTemplates[scope.selectedNodeTemplate.name].relationshipsMap[relationshipName].value.propertiesMap[propertyName].value = { value: propertyValue, definition: false };
              }
            },
            null,
            scope.selectedNodeTemplate,
            true
          );
        },

        remove: function(relationshipName, selectedNodeTemplate, callback) {
          var scope = this.scope;
          scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.relationshiptemplate.DeleteRelationshipOperation',
            nodeName: selectedNodeTemplate.name,
            relationshipName: relationshipName
          }, callback, null, selectedNodeTemplate.name);
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.relationships = instance;
        instance.init();
      };
    }
  ]); // modules
}); // define
