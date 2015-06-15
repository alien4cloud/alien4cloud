'use strict';

angular.module('alienUiApp').factory('svgServiceFactory', ['svgControlsFactory', 'browserService',
  function(svgControlsFactory, browserService) {

    function SvgGraph (containerElement, svgId, svgClass) {
      // create a controls instance for this svg (to move it and center it.).
      var toolbarElement = containerElement.append('div').attr('id', 'topology-control-button').attr('class', 'btn-toolbar pull-right').attr('role', 'toolbar').append('div').attr('class', 'btn-group');
      this.svg = containerElement.append('svg:svg');
      this.controls = svgControlsFactory.create(this.svg, toolbarElement);
      this.svg.attr('id', svgId);
      this.svg.attr('class', svgClass);
    }

    SvgGraph.prototype = {
      constructor: SvgGraph,

      /**
      * Trigger resize of the svg element.
      *
      * @param width The new width of the element.
      * @param height The new height of the element.
      */
      onResize: function(width, height) {
        var isFirefox = browserService.isBrowser('Firefox');
        if (isFirefox) {
          this.svg.attr('height', '100%');
        } else { // chrome, Safari...
          this.svg.attr('height', height);
        }
        this.svg.attr('width', '100%');
        this.controls.coordinateUtils.canvasWidth = width;
        this.controls.coordinateUtils.canvasHeight = height;
        this.controls.updateViewBox();
      }
    };

    return {
      create: function(containerElement, svgId, svgClass) {
        return new SvgGraph(containerElement, svgId, svgClass);
      }
    };
  } // function
]);
