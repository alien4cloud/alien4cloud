/** Manage selection of versions for topology edition. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('topoEditVersions', [ 'topologyServices', 'topologyRecoveryServices',
    function(topologyServices, topologyRecoveryServices) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;

        // initialize the version to be used.
        if(_.defined(scope.versionContext.versionName)) {
          this.setSelectedVersionByName(scope.versionContext.versionName);
        } else {
          // select the last version number (first in the array)
          scope.selectedVersion = scope.topologyVersions[0];
          scope.topologyId = scope.selectedVersion.id;
          scope.versionContext.topologyId = scope.topologyId;
          scope.versionContext.versionName = scope.selectedVersion.name;
        }
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,

        setSelectedVersionByName: function(name) {
          for (var i = 0; i < this.scope.topologyVersions.length; i++) {
            if (this.scope.topologyVersions[i].version === name) {
              this.scope.selectedVersionName = name;
              this.scope.selectedVersion = this.scope.topologyVersions[i];
              this.scope.topologyId = this.scope.selectedVersion.id;
              this.scope.versionContext.topologyId = this.scope.topologyId;
              this.scope.versionContext.versionName = this.scope.name;
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
            if(_.undefined(successResult.error)){
              instance.scope.refreshTopology(successResult.data);
              return;
            }

            //case there actually is an error
            topologyRecoveryServices.handleTopologyRecovery(successResult.data, instance.scope.topologyId, instance.scope.getLastOperationId(true)).then(function(recoveryResult){
              if(_.definedPath(recoveryResult, 'data')){
                instance.scope.refreshTopology(recoveryResult.data);
              }
            });

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
