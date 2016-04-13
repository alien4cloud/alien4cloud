/** Manage artifacts upload and reset for topology edition. */
define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('topoEditArtifacts', [ 'topologyServices', '$upload',
    function(topologyServices, $upload) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,

        doUpload: function(file, artifactId) {
          var uploadNodeTemplate = this.scope.selectedNodeTemplate;
          if (_.undefined(this.scope.uploads)) {
            this.scope.uploads = {};
          }
          this.scope.uploads[artifactId] = {
            'isUploading': true,
            'type': 'info'
          };
          var instance = this;
          $upload.upload({
            url: 'rest/latest/topologies/' + instance.scope.topology.topology.id + '/nodetemplates/' + uploadNodeTemplate.name + '/artifacts/' + artifactId,
            file: file
          }).progress(function(evt) {
            instance.scope.uploads[artifactId].uploadProgress = parseInt(100.0 * evt.loaded / evt.total);
          }).success(function(success) {
            if (!success.error) {
              instance.scope.uploads[artifactId].isUploading = false;
              instance.scope.uploads[artifactId].type = 'success';
              instance.scope.refreshTopology(success.data);
            }
          }).error(function(data, status) {
            instance.scope.uploads[artifactId].type = 'error';
            instance.scope.uploads[artifactId].error = {};
            instance.scope.uploads[artifactId].error.code = status;
            instance.scope.uploads[artifactId].error.message = 'An Error has occurred on the server!';
          });
        },

        onSelected: function($files, artifactId) {
          var file = $files[0];
          this.doUpload(file, artifactId);
        },

        // reset the artifact to the original nodetype value
        reset: function(artifactId) {
          var instance = this;
          topologyServices.nodeTemplate.artifacts.resetArtifact({
            topologyId: instance.scope.topology.topology.id,
            nodeTemplateName: instance.scope.selectedNodeTemplate.name,
            artifactId: artifactId
          }, function success(result) {
            if (result.error === null) {
              instance.scope.refreshTopology(result.data);
            }
          });
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.artifacts = instance;
      };
    }
  ]); // modules
}); // define
