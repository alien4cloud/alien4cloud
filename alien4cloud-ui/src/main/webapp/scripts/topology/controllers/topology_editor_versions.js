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
        var previousVersionFromCache = userContextServices.getTopologyContext(scope.versionContext.versions[0].applicationId);
        if(_.defined(previousVersionFromCache)){
          var previousVersionFromContext = _.find(scope.versionContext.versions, { 'id': previousVersionFromCache.version });
          this.setSelectedVersionVariant(previousVersionFromContext, previousVersionFromCache.variant);
        } else {
          // select the last version number (first in the array)
          this.setSelectedDefaultVersion(scope.versionContext.versions[0]);
        }
      };
      
      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,

        setSelectedDefaultVersion: function (version) {
          this.scope.userSelection.version = version;
          // legacy behavior
          if (_.defined(version.topologyVersions)) {
            // case there is topology versions pick the first one
            this.scope.userSelection.topologyVersion = _.findKey(version.topologyVersions, function (item) { return _.defined(item.archiveId); });
            this.scope.topologyId = version.topologyVersions[this.scope.userSelection.topologyVersion].archiveId;
          } else {
            this.scope.topologyId = this.scope.userSelection.version.id;
          }

          userContextServices.updateTopologyContext(version.applicationId, version, this.scope.userSelection.topologyVersion);
        },

        setSelectedVersionVariant: function(version, variant) {
          this.scope.userSelection.version = version;
          this.scope.userSelection.topologyVersion = variant;
          this.scope.topologyId = version.topologyVersions[variant].archiveId;

          userContextServices.updateTopologyContext(version.applicationId, version, variant);
        },

        change: function(version) {
          this.setSelectedDefaultVersion(version);
          this.refreshTopology();
        },

        changeTopologyVersion: function(selectedTopologyVersion) {
          this.setSelectedVersionVariant(this.scope.userSelection.version, selectedTopologyVersion);
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
