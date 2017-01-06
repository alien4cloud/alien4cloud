/*
* Shape in the renderer is responsible for drawing itself, defining optional container area and placing connectors.
*
* Connectors must have a reference to a target selector that will allow to find valid target connectors.
*
* Some shapes may contains other shapes. In such situations minimum Size of the shape may depends of inner components within the shape.
*/
define(function (require) {
  'use strict';

  var modules = require('modules');

  require('scripts/common/services/browser_service');
  require('scripts/common-graph/services/svg_controls_service');
  var _ = require('lodash');

  modules.get('a4c-common-graph', ['a4c-common']).factory('simpleShapeFactory', [
    function() {
      // Definition of a connector for the graph.
      function Connector() {
      }

      Connector.prototype = {
        constructor: Connector,
        // Flag to know if the connector can be used to start a connection (drag from)
        isDragable: true
      };

      function SimpleShape () {
        // Constructor for the simple shape.
      }

      SimpleShape.prototype = {
        constructor: SimpleShape,

        // x position of the shape.
        x: 0,
        // y position of the shape.
        y: 0,
        // Minimum width of the shape.
        minWidth: 0,
        // Current width of the shape.
        width: 0,
        // Minimum height of the shape.
        minHeight: 0,
        // Current height of the shape.
        height: 0,

        // container zone of the shape. This must be a square.
        container: {
          x: 0,
          y: 0
        },

        // List of connectors for the shape.
        connectors: []
      };

      return {
        create: function() {
          return new SimpleShape();
        }
      };
    } // function
  ]); // factory
}); // define
