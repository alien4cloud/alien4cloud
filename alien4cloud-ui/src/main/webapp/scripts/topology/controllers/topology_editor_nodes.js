/**
*  Service that provides functionalities to edit nodes in a topology.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/common/controllers/confirm_modal');

  modules.get('a4c-topology-editor').factory('topoEditNodes', ['toscaService', '$filter', '$uibModal', '$translate',
    function(toscaService, $filter, $uibModal, $translate) {
      var nodeNamePattern = '^\\w+$';

      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        /** Init method is called when the controller is ready. */
        init: function() {},
        /** Method triggered as a result of a on-drag (see drag and drop directive and node type search directive). */
        onDragged: function(e) {
          var nodeType = angular.fromJson(e.source);
          var evt = e.event;
          var hostNodeName = null;
          if (evt.target.hasAttribute('node-template-id')) {
            hostNodeName = evt.target.getAttribute('node-template-id');
          }
          this.add(nodeType, hostNodeName);
        },
        /** this has to be exposed to the scope as we cannot rely on drag and drop callbacks for ui tests */
        add: function(nodeType, hostNodeName) {
          var self = this;
          var nodeTemplateName = toscaService.generateNodeTemplateName(nodeType.elementId, this.scope.topology.topology.nodeTemplates);
          // Add node operation automatically change dependency version to higher so if different warn the user.
          var currentVersion = this.getDepVersionIfDifferent(nodeType.archiveName, nodeType.archiveVersion, this.scope.topology.topology.dependencies);
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
                    name: nodeType.archiveName,
                    current: currentVersion,
                    new: nodeType.archiveVersion
                  });
                }
              }
            });
            modalInstance.result.then(function () {
              self.doAddNodeTemplate(nodeTemplateName, nodeType, hostNodeName);
            });
          } else {
            this.doAddNodeTemplate(nodeTemplateName, nodeType, hostNodeName);
          }
        },
        /** Actually trigger the node template addition. */
        doAddNodeTemplate: function(nodeTemplateName, selectedNodeType, targetNodeTemplateName) {
          var scope = this.scope;
          // Add node operation automatically change dependency version to higher so if different warn the user.
          scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation',
            nodeName: nodeTemplateName,
            indexedNodeTypeId: selectedNodeType.id
          }, function(result) {
            if (_.undefined(result.error) && targetNodeTemplateName) {
              // drag a node on another node
              scope.relationships.autoOpenRelationshipModal(nodeTemplateName, targetNodeTemplateName);
            }
          }, null, nodeTemplateName);
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
          var scope = this.scope;
          // Update only when the name has changed
          scope.nodeTempNameEditError = null;

          if (!newName.match(nodeNamePattern)) {
            return $filter('translate')('APPLICATIONS.TOPOLOGY.INVALID_NAME');
          }

          if (scope.selectedNodeTemplate.name !== newName) {
            scope.execute({
                type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.RenameNodeOperation',
                nodeName: scope.selectedNodeTemplate.name,
                newName: newName
              }, null,
              function() { // error handling
                scope.nodeNameObj.val = scope.selectedNodeTemplate.name;
              }, scope.selectedNodeTemplate ? newName : undefined
            );
          } // if end
          scope.display.set('nodetemplate', true);
        },
        delete: function(nodeTemplName) {
          var scope = this.scope;
          scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation',
              nodeName: nodeTemplName
            },
            function(){ scope.display.displayAndUpdateVisualDimensions(['topology']); }
          );
        },
        /* Update properties of a node template */
        updateProperty: function(propertyDefinition, propertyName, propertyValue) {
          var scope = this.scope;

          var updatedNodeTemplate = scope.selectedNodeTemplate;
          return scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation',
              nodeName: scope.selectedNodeTemplate.name,
              propertyName: propertyName,
              propertyValue: propertyValue
            },
            function(result){
              if (_.undefined(result.error)) {
                updatedNodeTemplate.propertiesMap[propertyName].value = {value: propertyValue, definition: false};
              }
            },
            null,
            scope.selectedNodeTemplate.name,
            true
          );
        },
        /*duplicate a node in the topology. Also duplicate the hosted nodes hierarchy. Discard any relationship targeting a node out of the hosted hierarchy*/
        duplicate: function(nodeName) {
          var scope = this.scope;
          scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.DuplicateNodeOperation',
              nodeName: nodeName
            },
            null, null, nodeName
          );
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.nodes = instance;
      };
    }
  ]); // modules
}); // define
