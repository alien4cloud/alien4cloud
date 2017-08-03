/** Manage selection of versions for topology edition. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('topoEditVersions', [ 'topologyServices', 'topologyRecoveryServices','userContextServices',
    function(topologyServices, topologyRecoveryServices, userContextServices) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
        this.scope.userSelection = {};

        // initialize the version to be used.
        var previousVersion = userContextServices.getTopologyContext(scope.versionContext.versions[0].applicationId);
        if(_.defined(previousVersion)){
          this.setSelectedVersion(previousVersion.version);
        } else if(_.defined(scope.versionContext.versionName)) {
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
          this.scope.userSelection.version = version;
          var context = userContextServices.getTopologyContext(version.applicationId);
          if(_.defined(context) && _.isEqual(context.version.version, version.version)){
            this.scope.userSelection.topologyVersion = context.variant;
            this.scope.topologyId = context.version.topologyVersions[context.variant].archiveId;
          }else{
            // legacy behavior
            if(_.defined(version.topologyVersions)) {
              // case there is topology versions pick the first one
              this.scope.userSelection.topologyVersion = _.findKey(version.topologyVersions, function(item) {return _.defined(item.archiveId);});
              this.scope.topologyId = version.topologyVersions[this.scope.userSelection.topologyVersion].archiveId;
            } else {
              this.scope.topologyId = this.scope.userSelection.version.id;
            }
          }

          // useful in the following scenario:
          // the user do a full refresh on the topology (with F5)
          // then the user context is empty and to be need to be updated
          if(_.undefined(context)){
            userContextServices.updateTopologyContext(version.applicationId, version, this.scope.userSelection.topologyVersion);
          }
        },

        change: function(version) {
          userContextServices.updateTopologyContext(
            version.applicationId,
            version,
            version.version // default version
          );
          this.setSelectedVersion(version);
          this.refreshTopology();
        },
        changeTopologyVersion: function(selectedTopologyVersion) {
          var newSelectedVersion = this.scope.userSelection.version.topologyVersions[selectedTopologyVersion];
          userContextServices.updateTopologyContext(
            this.scope.userSelection.version.applicationId,
            this.scope.userSelection.version,
            selectedTopologyVersion
          );

          this.scope.userSelection.topologyVersion = selectedTopologyVersion;
          this.scope.topologyId = newSelectedVersion.archiveId;
          this.refreshTopology();
        },
        refreshTopology: function() {
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
