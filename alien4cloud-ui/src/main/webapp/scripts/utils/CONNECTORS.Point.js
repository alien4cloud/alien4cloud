// Manages the directions for connectors routing.
/* global CONNECTORS */

'use strict';

CONNECTORS.Point = function(x, y) {
  this.x = x;
  this.y = y;
};

CONNECTORS.Point.prototype = {
  constructor: CONNECTORS.Point,

  /**
  * Compute the manhattan distance between the current point and the other point.
  *
  * @param other The point to compute manhattan distance to.
  */
  manhattan: function(other) {
    return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
  },

  /**
  * Checks if the other point has same coordinates of the current point.
  *
  * @param other The point we want to compare to the current one.
  */
  equals: function(other) {
    return this.x === other.x && this.y === other.y;
  }
};
