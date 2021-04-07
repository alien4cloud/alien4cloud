define(function (require) {
  'use strict';

  var modules = require('modules');
  var d3 = require('d3');
  var _ = require('lodash');

  modules.get('a4c-common-graph').factory('svgControlsFactory', [function() {
      /**
      * Svg controll appends a tooltip element with buttons as well as .
      *
      * @param containerElement Container element is the element that must contains the svg element.
      */
      function SvgControls (svgElement, toolbarElement, graphControl) {
        this.moveStep = 50; // px shift on move

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
          instance.reset();
        });

        // defines a d3 behavior to manage viewport translations and zoom
        var self = this;

        this.zoomRect = svgElement.append('rect').attr('fill', 'white');
        this.svgGroup = svgElement.append('g');
        this.zoom = d3.behavior.zoom().on('zoom',
            function() {
              if (!isNaN(d3.event.scale)) {
                self.svgGroup.attr('transform', 'translate(' + d3.event.translate + ')' + 'scale(' + d3.event.scale + ')');
              }
            });
        if(_.defined(graphControl)) {
          graphControl.toRealCoords = function(coords) {
            var translate = instance.zoom.translate();
            return {
              x: (coords.x - translate[0]) / instance.zoom.scale(),
              y: (coords.y - translate[1]) / instance.zoom.scale()
            };
          };
        }
        svgElement.call(this.zoom);
      }

      SvgControls.prototype = {
        constructor: SvgControls,
        bbox: null,

        resize: function(width, height) {
          this.canvasWidth = width;
          this.canvasHeight = height;
          this.zoomRect.attr('width', width).attr('height', height);
          // TODO center
          this.reset();
        },

        reset: function() {
          // center and find optimal scale
          var x=0, y=0, scale = 1;
          if (_.defined(this.bbox)) {
            // center the drawing and set the scale if the drawing doesn't fit
            // compute ideal scale to display everything
            var widthScale = this.canvasWidth / this.bbox.width();
            var heightScale = this.canvasHeight / this.bbox.height();
            scale = widthScale < heightScale ? widthScale : heightScale;
            var margingLeft = (this.canvasWidth - this.bbox.width() * scale) / 2;
            var marginTop =  (this.canvasHeight - this.bbox.height() * scale) / 2;
            x = -(this.bbox.minX) * scale + margingLeft;
            y = -(this.bbox.minY) * scale + marginTop;
          }
          this.zoom.scale(scale);
          this.zoom.translate([x, y]);

          this.zoom.event(this.svgGroup);
        },

        updateBBox: function(bbox) {
          // set the BoundingBox of the drawing
          this.bbox = bbox;
        },

        translateLeft: function() {
          var translate = this.zoom.translate();
          translate[0] -= this.moveStep;
          this.zoom.translate(translate);
          this.zoom.event(this.svgGroup);
        },

        translateRight: function() {
          var translate = this.zoom.translate();
          translate[0] += this.moveStep;
          this.zoom.translate(translate);
          this.zoom.event(this.svgGroup);
        },

        translateUp: function() {
          var translate = this.zoom.translate();
          translate[1] += this.moveStep;
          this.zoom.translate(translate);
          this.zoom.event(this.svgGroup);
        },

        translateDown: function() {
          var translate = this.zoom.translate();
          translate[1] -= this.moveStep;
          this.zoom.translate(translate);
          this.zoom.event(this.svgGroup);
        },

        zoomOut: function() {
          this.zoom.scale(this.zoom.scale() * (1 - 0.15));
          this.zoom.event(this.svgGroup);
        },

        zoomIn: function() {
          this.zoom.scale(this.zoom.scale() * (1 + 0.15));
          this.zoom.event(this.svgGroup);
        }
      };

      return {
        create: function(svgElement, toolbarElement, graphControl) {
          return new SvgControls(svgElement, toolbarElement, graphControl);
        }
      };
    } // function
  ]); // factory
}); // define
