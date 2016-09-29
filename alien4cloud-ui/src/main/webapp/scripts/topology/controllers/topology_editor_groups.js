/** Group management for topology edition. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('topoEditGroups', ['$q', 'runtimeColorsService',
    function($q, runtimeColorsService) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        delete: function(groupName) {
          this.scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.groups.DeleteGroupOperation',
            groupName: groupName
          });
        },
        updateName: function(groupName, name) {
          var self = this;
          var d = $q.defer();
          this.scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.groups.RenameGroupOperation',
            groupName: groupName,
            newGroupName: name
          }, function(result) {
            if(result.data && result.data.error) {
              d.reject(result.data.error.message);
            } else {
              // update ui collapse for the new name
              if (self.scope.groupCollapsed[groupName]) {
                self.scope.groupCollapsed[name] = self.scope.groupCollapsed[groupName];
                delete self.scope.groupCollapsed[groupName];
              }
              d.resolve();
            }
          }, function(result) {
            if(result.data && result.data.error) {
              d.reject(result.data.error.message);
            } else {
              d.reject();
            }
          });
          return d.promise;
          // FIXME at this moment you may have errors in the browser console due to the fact that the topology has not been refreshed.
          // Scope apply should be suspended and triggered only when topology is refreshed.
        },
        removeMember: function(groupName, member) {
          this.scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.groups.RemoveGroupMemberOperation',
            groupName: groupName,
            nodeName: member
          });
        },
        isMemberOf: function(nodeName, groupId) {
          if (this.scope.selectedNodeTemplate) {
            return _.contains(this.scope.selectedNodeTemplate.groups, groupId);
          }
        },
        create: function(nodeName) {
          var self = this;
          this.scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation',
            groupName: nodeName,
            nodeName: nodeName
          }, function(result) {
            if (!result.error) {
              self.scope.groupCollapsed[nodeName] = { main: false, members: true, policies: true };
            }
          });
        },
        toggleMember: function(groupName, nodeName) {
          if (this.isMemberOf(nodeName, groupName)) {
            this.removeMember(groupName, nodeName);
          } else {
            this.scope.execute({
              type: 'org.alien4cloud.tosca.editor.operations.groups.AddGroupMemberOperation',
              groupName: groupName,
              nodeName: nodeName
            });
          }
        },

        getColorCss: function(groupId) {
          return runtimeColorsService.groupColorCss(this.scope.topology.topology, groupId);
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.groups = instance;
      };
    }
  ]); // modules
}); // define
