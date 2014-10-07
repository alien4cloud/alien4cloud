/* global $ */
'use strict';

angular.module('alienUiApp').factory('resizeServices', [ function() {
  // the default min width and height for the application
  var minWidth = 640;
  var minHeight = 200;

  return {
    register: function(callback, widthOffset, heightOffset) {
      var instance = this;
      window.onresize = function() {
        callback(instance.getWidth(widthOffset), instance.getHeight(heightOffset));
      };
    },

    getHeight : function(offset){
      var height = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
      if(height < minHeight) {
        height = minHeight;
      }
      height = height - offset;
      return height;
    },

    getWidth  : function(offset) {
      var width = $('.main-view').innerWidth();
      if(width < minWidth) {
        width = minWidth;
      }
      width = width - offset;
      return width;
    }
  };
}]);
