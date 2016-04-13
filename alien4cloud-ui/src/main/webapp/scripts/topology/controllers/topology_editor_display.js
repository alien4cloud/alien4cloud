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
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        /** Init method is called when the controller is ready. */
        init: function() {
          this.scope.view = 'RENDERED';

          this.scope.isNodeTemplateCollapsed = false;
          this.scope.isPropertiesCollapsed = false;
          this.scope.isRelationshipsCollapsed = false;
          this.scope.isRelationshipCollapsed = false;
          this.scope.isArtifactsCollapsed = false;
          this.scope.isArtifactCollapsed = false;
          this.scope.isRequirementsCollapsed = false;
          this.scope.isCapabilitiesCollapsed = false;

          // default values that are going to be refreshed automatically
          this.scope.dimensions = { width: 800, height: 600 };

          this.scope.displays = {
            catalog: { active: true, size: 500, selector: '#catalog-box' },
            dependencies: { active: false, size: 400, selector: '#dependencies-box' },
            inputs: { active: false, size: 400, selector: '#inputs-box' },
            artifacts: { active: false, size: 400 },
            groups: { active: false, size: 400 },
            substitutions: { active: false, size: 400 },
            component: { active: false, size: 500, selector: '#nodetemplate-details' }
          };

          var self = this;
          // Size management
          _.each(this.scope.displays, function(display) {
            if(_.defined(display.selector)) {
              var handlerSelector = display.selector + '-handler';
              $(display.selector).resizable({
                handles: {
                  w: $(handlerSelector)
                },
                resize: function( event, ui ) {
                  display.size = ui.size.width;
                  self.updateVisualDimensions();
                  self.scope.$digest();
                }
              });
            }
          });
          resizeServices.registerContainer(function(width, height) { self.onResize(width, height); }, '#topology-editor');
          this.updateVisualDimensions();
        },
        onResize: function(width, height) {
          this.scope.dimensions = {
            width: width,
            height: height
          };
          var maxWidth = (width - 100) / 2;
          _.each(this.scope.displays, function(display) {
            if(_.defined(display.selector)) {
              $(display.selector).resizable('option', 'maxWidth', maxWidth);
            }
          });
          this.updateVisualDimensions();
          this.scope.$digest();
        },
        updateVisualDimensions: function() {
          var instance = this, width = this.scope.dimensions.width - 20; // vertical menu
          _.each(this.scope.displays, function(display) {
            if(display.active) {
              width = width - display.size;
            }
          });
          this.scope.visualDimensions = {
            height: instance.scope.dimensions.height - 22,
            width: width
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
          var beforeComponentActive = this.scope.displays.component.active;
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

          if(beforeComponentActive && !this.scope.displays.component.active &&
            _.defined(this.scope.selectedNodeTemplate)) {
            this.scope.selectedNodeTemplate.selected = false;
            this.scope.selectedNodeTemplate = null;
            this.scope.triggerTopologyRefresh = {};
          }

          this.updateVisualDimensions();
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
