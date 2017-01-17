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
          for (var i = 0; i < this.scope.versionContext.versions.length; i++) {
            if (this.scope.versionContext.versions[i].version === scope.versionContext.versionName) {
              this.setSelectedVersion(this.scope.versionContext.versions[i]);
              break;
            }
          }
        } else {
          // select the last version number (first in the array)
          this.setSelectedVersion(scope.versionContext.versions[0]);
        }
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,

        setSelectedVersion: function(version) {
          this.scope.selectedVersion = version;
          if(_.defined(version.topologyVersions)) {
            // case there is topology versions pick the first one
            this.scope.selectedTopologyVersion = _.findKey(version.topologyVersions, function(item) {return _.defined(item.archiveId);});
            this.scope.topologyId = version.topologyVersions[this.scope.selectedTopologyVersion].archiveId;
          } else {
            this.scope.topologyId = this.scope.selectedVersion.id;
          }
        },

        change: function(version) {
          this.setSelectedVersion(version);
          this.refreshTopology();
        },
        changeTopologyVersion: function(selectedTopologyVersion) {
          this.scope.selectedTopologyVersion = selectedTopologyVersion;
          this.scope.topologyId = this.scope.selectedVersion.topologyVersions[this.scope.selectedTopologyVersion].archiveId;
          this.refreshTopology();
        },
        refreshTopology() {
          var instance = this;
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
