define(function (require) {
  'use strict';

  var modules = require('modules');
  var $ = require('jquery');
  var _ = require('lodash');

  modules.get('a4c-common').factory('resizeServices', ['$timeout', function($timeout) {
    // the default min width and height for the application
    var minWidth = 640;
    var minHeight = 200;

    return {
      registerContainer: function(callback, selector) {
        var instance = this;
        $timeout(function() { // make sure w wait for the DOM to be ready before, because $() does not return a promise
          var container = $(selector);
          window.onresize = function() {
            if (container.size()) {
              var offsets = container.offset();
              if (_.defined(offsets) && _.defined(offsets.top) && _.defined(offsets.left)) {
                callback(instance.getWidth(offsets.left), instance.getHeight(offsets.top));
              }
            }
          };
        });
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
