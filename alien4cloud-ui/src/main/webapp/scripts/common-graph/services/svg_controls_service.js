define(function (require) {
  'use strict';

  var modules = require('modules');

  var d3 = require('d3');
  require('scripts/common-graph/services/coordinates_util_service');

  modules.get('a4c-common-graph').factory('svgControlsFactory', ['coordinateUtilsFactory',
    function(coordinateUtilsFactory) {

      /**
      * Svg controll appends a tooltip element with buttons as well as .
      *
      * @param containerElement Container element is the element that must contains the svg element.
      */
      function SvgControls (svgElement, toolbarElement) {
        this.svgElement = svgElement;
        this.coordinateUtils = coordinateUtilsFactory.create(svgElement.node().clientWidth, svgElement.node().clientHeight);
        this.moveStep = 50; // px shift on move
        this.lastX = null;
        this.lastY = null;

        var instance = this;

        // TODO buttons creation should actually be moved to a directive.
        // moveLeft command
        var btnLeft = toolbarElement.append('button');
        btnLeft.attr('class', 'btn btn-default btn-xs');
        btnLeft.append('i').attr('class', 'fa fa-chevron-left');
        btnLeft.on('click', function() {
          instance.translateLeft();
        });

        // moveRight command
        var btnRight = toolbarElement.append('button');
        btnRight.attr('class', 'btn btn-default btn-xs');
        btnRight.append('i').attr('class', 'fa fa-chevron-right');
        btnRight.on('click', function() {
          instance.translateRight();
        });

        // moveUp command
        var btnUp = toolbarElement.append('button');
        btnUp.attr('class', 'btn btn-default btn-xs');
        btnUp.append('i').attr('class', 'fa fa-chevron-up');
        btnUp.on('click', function() {
          instance.translateUp();
        });

        // moveDown command
        var btnDown = toolbarElement.append('button');
        btnDown.attr('class', 'btn btn-default btn-xs');
        btnDown.append('i').attr('class', 'fa fa-chevron-down');
        btnDown.on('click', function() {
          instance.translateDown();
        });

        // zoom in command
        var btnZoomIn = toolbarElement.append('button');
        btnZoomIn.attr('class', 'btn btn-default btn-xs');
        btnZoomIn.attr('id', 'btn-topology-zoomin');
        btnZoomIn.append('i').attr('class', 'fa fa-search-plus');
        btnZoomIn.on('click', function() {
          instance.zoomIn();
        });

        // zoom out command
        var btnZoomOut = toolbarElement.append('button');
        btnZoomOut.attr('class', 'btn btn-default btn-xs');
        btnZoomOut.attr('id', 'btn-topology-zoomout');
        btnZoomOut.append('i').attr('class', 'fa fa-search-minus');
        btnZoomOut.on('click', function() {
          instance.zoomOut();
        });

        // center / init view command
        var btnReset = toolbarElement.append('button');
        btnReset.attr('class', 'btn btn-default btn-xs');
        btnReset.attr('id', 'btn-topology-reset');
        btnReset.append('i').attr('class', 'fa fa-crosshairs');
        btnReset.on('click', function() {
          instance.center();
        });

        svgElement.on('mouseup', function() {
          svgElement.on('mousemove', null);
        });

        svgElement.on('mousedown', function() {
          instance.down(d3.event.clientX, d3.event.clientY);
        });

        svgElement.on('mouseout', function() {
          svgElement.on('mousemove', null);
        });
      }

      SvgControls.prototype = {
        constructor: SvgControls,

        /**
        * Update the view box of the svg element to match the current coordinates.
        */
        updateViewBox: function() {
          this.svgElement.attr('viewBox',
            this.coordinateUtils.x + ' ' + this.coordinateUtils.y + ' ' + this.coordinateUtils.width() + ' ' + this.coordinateUtils.height());
          var version = navigator.userAgent.match(/Version\/(.*?)\s/) || navigator.userAgent.match(/Chrome\/(\d+)/);
          if ((navigator.vendor === 'Apple Computer, Inc.') ||
            (navigator.vendor === 'Google Inc.' && version && version[1] < 8)) {
            var rect = this.svgElement.append('rect');
            setTimeout(function() {
              rect.remove();
            });
          }
        },

        translateLeft: function() {
          this.coordinateUtils.translate(-this.moveStep, 0);
          this.updateViewBox();
        },

        translateRight: function() {
          this.coordinateUtils.translate(this.moveStep, 0);
          this.updateViewBox();
        },

        translateUp: function() {
          this.coordinateUtils.translate(0, -this.moveStep);
          this.updateViewBox();
        },

        translateDown: function() {
          this.coordinateUtils.translate(0, this.moveStep);
          this.updateViewBox();
        },

        zoomOut: function() {
          this.coordinateUtils.zoomOut();
          this.updateViewBox();
        },

        zoomIn: function() {
          this.coordinateUtils.zoomIn();
          this.updateViewBox();
        },

        center: function() {
          this.coordinateUtils.reset();
          this.updateViewBox();
        },

        move: function(x, y) {
          var tx = x - this.lastX;
          var ty = y - this.lastY;
          this.lastX = x;
          this.lastY = y;
          this.coordinateUtils.translate(tx, ty);
          this.updateViewBox();
        },

        down: function(x, y) {
          this.lastX = x;
          this.lastY = y;
          var instance = this;
          this.svgElement.on('mousemove', function() {
            instance.move(d3.event.clientX, d3.event.clientY);
          });
        }
      };

      return {
        create: function(svgElement, toolbarElement) {
          return new SvgControls(svgElement, toolbarElement);
        }
      };
    } // function
  ]); // factory
}); // define
