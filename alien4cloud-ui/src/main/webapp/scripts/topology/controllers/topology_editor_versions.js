/** Manage selection of versions for topology edition. */
define(function (require) {
  'use strict';
  var modules = require('modules');

  modules.get('a4c-topology-editor').factory('topoEditVersions', [ 'topologyServices',
    function(topologyServices) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,

        setSelectedVersionByName: function(name) {
          for (var i = 0; i < this.scope.appVersions.length; i++) {
            if (this.scope.appVersions[i].version === name) {
              this.scope.selectedVersionName = name;
              this.scope.selectedVersion = this.scope.appVersions[i];
              this.scope.topologyId = this.scope.selectedVersion.topologyId;
              break;
            }
          }
        },

        change: function(selectedVersion) {
          var instance = this;
          this.setSelectedVersionByName(selectedVersion.version);
          topologyServices.dao.get({
            topologyId: instance.scope.topologyId
          }, function(successResult) {
            instance.scope.refreshTopology(successResult.data);
          });
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.versions = instance;

      };
    }
  ]); // modules
}); // define
