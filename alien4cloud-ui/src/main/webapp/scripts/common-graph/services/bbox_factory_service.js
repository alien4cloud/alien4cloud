define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common-graph').factory('bboxFactory', function() {
    var BoundingBox = function(x, y, width, height) {
      if(_.defined(x)) {
        this.minX = x;
        this.minY = y;
        this.maxX = x + width;
        this.maxY = y + height;
      } else {
        this.minX = null;
        this.minY = null;
        this.maxX = null;
        this.maxY = null;
      }
    };

    BoundingBox.prototype = {
      constructor: BoundingBox,

      addPoint: function(x, y) {
        if(this.minX === null) {
          // initialize bbox
          this.minX = x;
          this.minY = y;
          this.maxX = x;
          this.maxY = y;
        } else {
          if(x < this.minX) {
            this.minX = x;
          }
          if(y < this.minY) {
            this.minY = y;
          }
          if(x > this.maxX) {
            this.maxX = x;
          }
          if(y > this.maxY) {
            this.maxY = y;
          }
        }
      },

      add: function(bbox) {
        this.addPoint(bbox.minX, bbox.minY);
        this.addPoint(bbox.maxX, bbox.maxY);
      },

      addRectFromCenter: function(x, y, width, height) {
        // left bottom point
        var rectMinX = x - width / 2;
        var rectMinY = y - height / 2;
        // right top point
        var rectMaxX = x + width / 2;
        var rectMaxY = y + height / 2;

        // add points
        this.addPoint(rectMinX, rectMinY);
        this.addPoint(rectMaxX, rectMaxY);
      },

      x: function() {
        return this.minX;
      },

      y: function() {
        return this.minY;
      },

      width: function() {
        return this.maxX - this.minX;
      },

      height: function() {
        return this.maxY - this.minY;
      },

      move: function(x, y) {
        var tx = x - this.minX, ty = y - this.minY;
        this.minX = x;
        this.minY = y;
        this.maxX += tx;
        this.maxY += ty;
      },

      clone: function() {
        var clone = new BoundingBox();
        clone.minX = this.minX;
        clone.minY = this.minY;
        clone.maxX = this.maxX;
        clone.maxY = this.maxY;
        return clone;
      },

      cloneWithPadding: function(padding) {
        var clone = new BoundingBox();
        clone.minX = this.minX - padding;
        clone.minY = this.minY - padding;
        clone.maxX = this.maxX + padding;
        clone.maxY = this.maxY + padding;
        return clone;
      },

      containsBBox: function(bbox) {
        return this.contains({x: bbox.minX, y: bbox.minY}) && this.contains({x: bbox.maxX, y: bbox.maxY});
      },

      contains: function(point) {
        if(point.x < this.minX || this.maxX < point.x) {
          return false;
        }
        if(point.y < this.minY || this.maxY < point.y) {
          return false;
        }
        return true;
      }
    };

    return {
      create: function(x, y, width, height) {
        return new BoundingBox(x, y, width, height);
      }
    };
  });// factory
}); // define
