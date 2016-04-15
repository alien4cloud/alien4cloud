/**
*  Service that provides functionalities to edit nodes in a topology.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('topoEditNodes', [ 'topologyServices', 'toscaService', '$filter',
    function(topologyServices, toscaService, $filter) {
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
          // find if the node type has been dragged on another node template (so we can generate hosted on relationship).
          var evt = e.event;
          var hostNodeName = null;
          if (evt.target.hasAttribute('node-template-id')) {
            hostNodeName = evt.target.getAttribute('node-template-id');
          }
          this.add(nodeType, hostNodeName);
        },
        /** this has to be exposed to the scope as we cannot rely on drag and drop callbacks for ui tests */
        add: function(nodeType, hostNodeName) {
          var nodeTemplName = toscaService.generateNodeTemplateName(nodeType.elementId, this.scope.topology.topology.nodeTemplates);
          this.doAddNodeTemplate(nodeTemplName, nodeType, hostNodeName);
        },
        /** Actually trigger the node template addition. */
        doAddNodeTemplate: function(nodeTemplateName, selectedNodeType, targetNodeTemplateName) {
          var scope = this.scope;
          var nodeTemplateRequest = {
            'name': nodeTemplateName,
            'indexedNodeTypeId': selectedNodeType.id
          };
          topologyServices.nodeTemplate.add({
            topologyId: scope.topology.topology.id
          }, angular.toJson(nodeTemplateRequest), function(result) {
            if (!result.error) {
              // refresh ui elements.
              scope.refreshTopology(result.data, nodeTemplateName);
              // if we have a target node we should try to create a hosted on relationship
              if (targetNodeTemplateName) {
                // drag a node on another node
                scope.relationships.autoOpenRelationshipModal(nodeTemplateName, targetNodeTemplateName);
              }
            }
          });
        },
        /* Update node template name */
        updateName: function(newName) {
          var scope = this.scope;
          // Update only when the name has changed
          scope.nodeTempNameEditError = null;

          if (!newName.match(nodeNamePattern)) {
            return $filter('translate')('APPLICATIONS.TOPOLOGY.INVALID_NODE_NAME');
          }

          if (scope.selectedNodeTemplate.name !== newName) {
            topologyServices.nodeTemplate.updateName({
              topologyId: scope.topology.topology.id,
              nodeTemplateName: scope.selectedNodeTemplate.name,
              newName: newName
            }, function(resultData) {
              if (resultData.error === null) {
                scope.refreshTopology(resultData.data, scope.selectedNodeTemplate ? newName : undefined);
              }
            }, function() {
              scope.nodeNameObj.val = scope.selectedNodeTemplate.name;
            });
          } // if end
          scope.display.set('component', true);
        },

        /* Update properties of a node template */
        updateProperty: function(propertyDefinition, propertyName, propertyValue) {
          var scope = this.scope;
          var updatePropsObject = {
            'propertyName': propertyName,
            'propertyValue': propertyValue
          };

          var updatedNodeTemplate = scope.selectedNodeTemplate;
          return topologyServices.nodeTemplate.updateProperty({
            topologyId: scope.topology.topology.id,
            nodeTemplateName: scope.selectedNodeTemplate.name
          }, angular.toJson(updatePropsObject), function(saveResult) {
            // update the selectedNodeTemplate properties locally
            if (_.undefined(saveResult.error)) {
              updatedNodeTemplate.propertiesMap[propertyName].value = {value: propertyValue, definition: false};
              scope.yaml.refresh();
            }
          }).$promise;
        },

        delete: function(nodeTemplName) {
          var scope = this.scope;
          topologyServices.nodeTemplate.remove({
            topologyId: scope.topology.topology.id,
            nodeTemplateName: nodeTemplName
          }, function(result) {
            // for refreshing the ui
            scope.refreshTopology(result.data);
            scope.display.displayOnly(['topology']);
          });
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.nodes = instance;
      };
    }
  ]); // modules
}); // define
