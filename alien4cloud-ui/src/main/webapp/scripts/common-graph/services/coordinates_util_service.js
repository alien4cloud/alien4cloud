define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common-graph').factory('coordinateUtilsFactory', function() {
    function CoordinateUtils (canvasWidth, canvasHeight) {
      this.canvasWidth = canvasWidth;
      this.canvasHeight = canvasHeight;
      this.x = 0;
      this.y = 0;
      this.bbox = null;
      this.scale = 1;
      this.scaleStep = 15; // percentage to be applied on scale on zoon in/out
    }

    CoordinateUtils.prototype = {
      constructor: CoordinateUtils,

      reset: function() {
        this.x = 0;
        this.y = 0;
        this.scale = 1;
        if (this.bbox !== null) {
          // center the drawing and set the scale if the drawing doesn't fit
          // compute ideal scale to display everything
          var widthScale = this.canvasWidth / this.bbox.width();
          var heightScale = this.canvasHeight / this.bbox.height();
          var newScale = widthScale < heightScale ? widthScale : heightScale;
          newScale = this.scale < newScale ? this.scale : newScale;

          this.scale = newScale;
          var halfWidth = this.width() / 2;
          var halfHeight = this.height() / 2;
          var centerX = this.bbox.minX + this.bbox.width() / 2;
          var centerY = this.bbox.minY + this.bbox.height() / 2;
          this.x = centerX - halfWidth;
          this.y = centerY - halfHeight;
        }
      },

      center: function() {
        if (this.bbox !== null) {
          // center the drawing and set the scale if the drawing doesn't fit
          this.x = -this.canvasWidth / 2 + (this.bbox.maxX - this.bbox.minX) / 2;
          this.y = -this.canvasHeight / 2 + (this.bbox.maxY - this.bbox.minY) / 2;
          // Little trick to re-zoom to what it was before
          var oldScale = this.scale;
          this.scale = 1;
          this.scaleChange(oldScale);
        }
      },

      width: function() {
        return this.canvasWidth / this.scale;
      },

      height: function() {
        return this.canvasHeight / this.scale;
      },

      zoomIn: function() {
        var newScale = this.scale * (1 + this.scaleStep / 100);
        this.scaleChange(newScale);
      },

      zoomOut: function() {
        var newScale = this.scale * (1 - this.scaleStep / 100);
        this.scaleChange(newScale);
      },

      scaleChange: function(newScale) {
        var centerX = this.x + this.width() / 2;
        var centerY = this.y + this.height() / 2;

        this.scale = newScale;

        this.x = centerX - this.width() / 2;
        this.y = centerY - this.height() / 2;
      },

      translate: function(tx, ty) {
        this.x -= (tx / this.scale);
        this.y -= (ty / this.scale);
      },

      uiToRealCoords: function(x, y) {
        return {
          'x': (x / this.scale) + this.x,
          'y': (y / this.scale) + this.y
        };
      }
    };

    return {
      create: function(canvasWidth, canvasHeight) {
        return new CoordinateUtils(canvasWidth, canvasHeight);
      }
    };
  }); // factory
}); // define
