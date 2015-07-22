/**
*  Topology editor display controller. This service is responsible for augmenting the editor scope to manage elements that should be displayed and resize.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');
  var $ = require('jquery');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('topoEditDisplay', [ 'resizeServices',
    function(resizeServices) {
      var resizableSelectors = ['#nodetemplate-details', '#catalog-box', '#dependencies-box', '#inputs-box'];

      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        /** Init method is called when the controller is ready. */
        init: function() {
          this.scope.view = 'RENDERED';
          this.scope.dimensions = {
            height: 50,
            width: 50
          };
          this.scope.displays = {
            topology: { active: true },
            catalog: { active: true, size: 500 },
            dependencies: { active: false, size: 400 },
            inputs: { active: false, size: 400 },
            artifacts: { active: false, size: 400 },
            groups: { active: false, size: 400 },
            substitutions: { active: false, size: 400 },
            component: { active: false, size: 400 }
          };
          this.scope.isNodeTemplateCollapsed = false;
          this.scope.isPropertiesCollapsed = false;
          this.scope.isRelationshipsCollapsed = false;
          this.scope.isRelationshipCollapsed = false;
          this.scope.isArtifactsCollapsed = false;
          this.scope.isArtifactCollapsed = false;
          this.scope.isRequirementsCollapsed = false;
          this.scope.isCapabilitiesCollapsed = false;
          // Size management
          for (var i = 0; i < resizableSelectors.length; i++) {
            var handlerSelector = resizableSelectors[i] + '-handler';
            $(resizableSelectors[i]).resizable({
              handles: {
                w: $(handlerSelector)
              }
            });
          }
          var instance = this;
          resizeServices.registerContainer(function(width, height) { instance.onResize(width, height); }, '#topology-editor');
          this.updateVisualDimensions();
        },
        onResize: function(width, height) {
          this.scope.dimensions = {
            width: width,
            height: height
          };
          this.updateVisualDimensions();
          var maxWidth = (width - 100) / 2;
          for (var i = 0; i < resizableSelectors.length; i++) {
            $(resizableSelectors[i]).resizable('option', 'maxWidth', maxWidth);
          }
          this.scope.$apply();
        },
        updateVisualDimensions: function() {
          var instance = this;
          this.scope.visualDimensions = {
            height: instance.scope.dimensions.height - 22,
            width: instance.scope.dimensions.width
          };
        },
        displayOnly: function(displays) {
          for (var displayName in this.scope.displays) {
            if (this.scope.displays.hasOwnProperty(displayName)) {
              this.scope.displays[displayName].active = _.contains(displays, displayName);
            }
          }
        },
        set: function(displayName, active) {
          if (this.scope.displays[displayName].active !== active) {
            this.toggle(displayName);
          }
        },
        toggle: function(displayName) {
          this.scope.displays[displayName].active = !this.scope.displays[displayName].active;
          // Specific rules for displays which are logically linked
          if (this.scope.displays[displayName].active) {
            switch (displayName) {
              case 'catalog':
                this.displayOnly(['topology', 'catalog']);
                break;
              case 'dependencies':
                this.displayOnly(['topology', 'dependencies']);
                break;
              case 'inputs':
                if (!this.scope.displays.component.active) {
                  this.displayOnly(['topology', 'inputs']);
                } else {
                  this.displayOnly(['topology', 'component', 'inputs']);
                }
                break;
              case 'groups':
                if (!this.scope.displays.component.active) {
                  this.displayOnly(['topology', 'groups']);
                } else {
                  this.displayOnly(['topology', 'component', 'groups']);
                }
                break;
              case 'artifacts':
                if (!this.scope.displays.component.active) {
                  this.displayOnly(['topology', 'artifacts']);
                } else {
                  this.displayOnly(['topology', 'component', 'artifacts']);
                }
                break;
              case 'component':
                if (!this.scope.displays.inputs.active) {
                  this.displayOnly(['topology', 'component']);
                } else {
                  this.displayOnly(['topology', 'component', 'inputs']);
                }
                break;
              case 'substitutions':
                if (!this.scope.displays.component.active) {
                  this.displayOnly(['topology', 'substitutions']);
                } else {
                  this.displayOnly(['topology', 'component', 'substitutions']);
                }
                break;
            }
          }
        }
      };

      return function(scope) {
        var instance = new TopologyEditorMixin(scope);
        scope.display = instance;
        instance.init();
      };
    }
  ]); // modules
}); // define
