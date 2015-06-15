/* global UTILS */

'use strict';

UTILS.BoundingBox = function(x, y, width, height) {
  if(UTILS.isDefinedAndNotNull(x)) {
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

UTILS.BoundingBox.prototype = {
  constructor: UTILS.BoundingBox,

  addPoint: function(x, y) {
    if(this.minX === null) {
      // initialize bbox
      this.minX = x;
      this.minY = y;
      this.maxX = x;
      this.maxX = y;
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

  width: function() {
    return this.maxX - this.minX;
  },

  height: function() {
    return this.maxY - this.minY;
  },

  clone: function() {
    var clone = new UTILS.BoundingBox();
    clone.minX = this.minX;
    clone.minY = this.minY;
    clone.maxX = this.maxX;
    clone.maxY = this.maxY;
    return clone;
  },

  cloneWithPadding: function(padding) {
    var clone = new UTILS.BoundingBox();
    clone.minX = this.minX - padding;
    clone.minY = this.minY - padding;
    clone.maxX = this.maxX + padding;
    clone.maxY = this.maxY + padding;
    return clone;
  }
};
