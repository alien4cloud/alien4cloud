/**
*  Topology editor display controller. This service is responsible for augmenting the editor scope to manage elements that should be displayed and resize.
*/
define(function (require) {
  'use strict';
  var modules = require('modules');
  var $ = require('jquery');
  var _ = require('lodash');

  require('scripts/common/services/resize_services');

  modules.get('a4c-topology-editor').factory('topoEditDisplay', [ 'resizeServices',
    function(resizeServices) {
      var TopologyEditorMixin = function(scope) {
        this.scope = scope;
      };

      TopologyEditorMixin.prototype = {
        constructor: TopologyEditorMixin,
        /** Init method is called when the controller is ready. */
        init: function(container) {
          this.scope.view = 'RENDERED';
          // default values that are going to be refreshed automatically
          this.scope.dimensions = { width: 800, height: 600 };

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
          resizeServices.registerContainer(function(width, height) { self.onResize(width, height); }, container);
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
        // display given elements and hide others except keepDisplays if active
        displayOnly: function(displays, keepDisplays) {
          _.each(this.scope.displays, function(display, displayName) {
            if(!_.includes(keepDisplays, displayName)) {
              display.active = _.includes(displays, displayName);
            }
          });
        },
        displayAndUpdateVisualDimensions: function(displays, keepDisplays) {
          this.displayOnly(displays, keepDisplays);
          this.scope.$broadcast('displayUpdate', { displays: this.scope.displays });
          this.updateVisualDimensions();
        },
        set: function(displayName, active) {
          if (this.scope.displays[displayName].active !== active) {
            this.toggle(displayName);
          }
        },
        toggle: function(displayName) {
          var targetDisplay = this.scope.displays[displayName];
          targetDisplay.active = !targetDisplay.active;
          // When some display are active some other may have not to be displayed.
          if (targetDisplay.active) {
            this.displayOnly(targetDisplay.only, targetDisplay.keep);
          }

          this.scope.$broadcast('displayUpdate', { displays: this.scope.displays });
          this.updateVisualDimensions();
        }
      };

      return function(scope, container) {
        var instance = new TopologyEditorMixin(scope);
        scope.display = instance;
        instance.init(container);
      };
    }
  ]); // modules
}); // define
