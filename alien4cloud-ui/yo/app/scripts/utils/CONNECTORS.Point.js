// Manages the directions for connectors routing.
/* global CONNECTORS */

'use strict';

CONNECTORS.Point = function(x, y) {
  this.x = x;
  this.y = y;
};

CONNECTORS.Point.prototype = {
  constructor: CONNECTORS.Point,

  manhattan: function(other) {
    return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
  },

  equals: function(other) {
    return this.x === other.x && this.y === other.y;
  }
};
