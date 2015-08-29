/** Service that provides functionalities to edit nodes in a topology. */
define(function (require) {
  'use strict';
  var modules = require('modules');

  modules.get('a4c-topology-editor').factory('topoEditYaml', [ 'topologyServices', '$timeout',
    function(topologyServices, $timeout) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };
      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        /** Init method is called when the controller is ready. */
        init: function() {
          this.scope.yaml.content = '';
        },
        display: function() {
          this.scope.view = 'YAML';
          this.refresh();
        },
        refresh: function() {
          var instance = this;
          if (this.scope.view !== 'YAML') {
            return;
          }
          // TODO Can we still have a topology editor without a version specified ?
          var currentTopologyId = (this.scope.selectedVersion) ? this.scope.selectedVersion.topologyId : this.scope.topologyId;
          topologyServices.getYaml({
            topologyId: currentTopologyId
          }, function(result) {
            instance.update(result.data);
          });
        },
        aceLoaded: function(editor) {
          this.aceEditor = editor;
        },
        update: function(content) {
          var instance = this;
          var firstVisibleRow = (this.aceEditor) ? this.aceEditor.getFirstVisibleRow() : undefined;
          this.scope.yaml.content = content;
          $timeout(function() {
            if (instance.aceEditor && firstVisibleRow) {
              instance.aceEditor.scrollToLine(firstVisibleRow);
            }
          });
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.yaml = instance;
        instance.init();
      };
    }
  ]); // modules
}); // define
