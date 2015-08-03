/**
*  Topology editor display controller. This service is responsible for augmenting the editor scope to manage elements that should be displayed and resize.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('topoEditRelationships', [ 'topologyServices', '$modal', 'nodeTemplateService',
    function(topologyServices, $modal, nodeTemplateService) {
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
        doAddRelationship: function(openedOnElementName, relationshipResult, requirementName, requirementType) {
          var scope = this.scope;
          var addRelationshipNodeTemplate = scope.topology.topology.nodeTemplates[openedOnElementName];
          if (!addRelationshipNodeTemplate.relationships) {
            addRelationshipNodeTemplate.relationships = {};
          }
          var relationshipTemplate = {
            type: relationshipResult.relationship.elementId,
            target: relationshipResult.target,
            requirementName: requirementName,
            requirementType: requirementType,
            targetedCapabilityName: relationshipResult.targetedCapabilityName
          };
          var addRelationshipRequest = {
            relationshipTemplate: relationshipTemplate,
            archiveName: relationshipResult.relationship.archiveName,
            archiveVersion: relationshipResult.relationship.archiveVersion
          };
          topologyServices.relationshipDAO.add({
            topologyId: scope.topology.topology.id,
            nodeTemplateName: openedOnElementName,
            relationshipName: relationshipResult.name
          }, angular.toJson(addRelationshipRequest), function(result) {
            // for refreshing the ui
            scope.refreshTopology(result.data, openedOnElementName);
          });
          scope.display.set('component', true);
        },

        openSearchRelationshipModal: function(sourceNodeTemplateName, requirementName, targetNodeTemplateName,
          targetedCapability) {
          var scope = this.scope;
          var instance = this;

          var sourceNodeTemplate = scope.topology.topology.nodeTemplates[sourceNodeTemplateName];
          var requirement = sourceNodeTemplate.requirementsMap[requirementName].value;
          if (!requirement.canAddRel.yes) {
            return; // TODO we must display an error message...
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
            scope: scope
          });

          modalInstance.result.then(function(relationshipResult) {
            instance.doAddRelationship(sourceNodeTemplateName, relationshipResult, requirementName, requirement.type);
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
            topologyServices.relationship.updateName({
              topologyId: scope.topology.topology.id,
              nodeTemplateName: scope.selectedNodeTemplate.name,
              relationshipName: oldName,
              newName: newName
            }, function(resultData) {
              if (resultData.error === null) {
                scope.refreshTopology(resultData.data, scope.selectedNodeTemplate ? scope.selectedNodeTemplate.name : undefined);
                delete scope.relNameObj[oldName];
              }
            }, function() {
              scope.relNameObj[oldName] = oldName;
            });
          } // if end
        },

        updateRelationshipProperty: function(propertyDefinition, propertyValue, relationshipType, relationshipName) {
          var scope = this.scope;
          var propertyName = propertyDefinition.name;
          var updateIndexedTypePropertyRequest = {
            'propertyName': propertyName,
            'propertyValue': propertyValue,
            'type': relationshipType
          };

          return topologyServices.relationship.updateProperty({
            topologyId: scope.topology.topology.id,
            nodeTemplateName: scope.selectedNodeTemplate.name,
            relationshipName: relationshipName
          }, angular.toJson(updateIndexedTypePropertyRequest), function() {
            // update the selectedNodeTemplate properties locally
            scope.topology.topology.nodeTemplates[scope.selectedNodeTemplate.name].relationshipsMap[relationshipName].value.propertiesMap[propertyName].value = {
              value: propertyValue,
              definition: false
            };
            scope.yaml.refresh();
          }).$promise;
        },

       remove: function(relationshipName, selectedNodeTemplate) {
         var scope = this.scope;
         topologyServices.relationshipDAO.remove({
           topologyId: scope.topology.topology.id,
           nodeTemplateName: selectedNodeTemplate.name,
           relationshipName: relationshipName
         }, function(result) {
           if (result.error === null) {
             scope.refreshTopology(result.data, selectedNodeTemplate.name);
           }
         });
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
