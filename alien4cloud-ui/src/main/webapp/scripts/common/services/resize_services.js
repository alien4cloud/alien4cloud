define(function (require) {
  'use strict';

  var modules = require('modules');
  var $ = require('jquery');

  modules.get('a4c-common').factory('resizeServices', ['$timeout', function($timeout) {
    // the default min width and height for the application
    var minWidth = 640;
    var minHeight = 200;

    return {
      registerContainer: function(callback, selector) {
        var container = $(selector);
        var instance = this;
        window.onresize = function() {
          if (container.size()) {
            var offsets = container.offset();
            if (offsets && offsets.top && offsets.left) {
              callback(instance.getWidth(offsets.left), instance.getHeight(offsets.top));
            }
          }
        };
        this.initSize();
      },

      register: function(callback, widthOffset, heightOffset) {
        var instance = this;
        window.onresize = function() {
          callback(instance.getWidth(widthOffset), instance.getHeight(heightOffset));
        };
        this.initSize();
      },

      initSize: function() {
        $timeout(function() {
          window.onresize();
        });
      },

      getHeight : function(offset){
        var height = $(window).height();
        if(height < minHeight) {
          height = minHeight;
        }
        return height - offset;
      },

      getWidth  : function(offset) {
        var width = $(window).width();
        if(width < minWidth) {
          width = minWidth;
        }
        return width - offset;
      }
    };
  }]); // factory
}); // define
