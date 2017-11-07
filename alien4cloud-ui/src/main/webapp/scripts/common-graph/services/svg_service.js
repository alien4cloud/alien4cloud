define(function (require) {
  'use strict';

  var modules = require('modules');

  require('scripts/common/services/browser_service');
  require('scripts/common-graph/services/svg_controls_service');
  var _ = require('lodash');

  modules.get('a4c-common-graph', ['a4c-common']).factory('svgServiceFactory', ['svgControlsFactory',
    function(svgControlsFactory) {

      function SvgGraph (containerElement, svgId, svgClass, toolbarContainer, graphControl) {
        // create a controls instance for this svg (to move it and center it.).
        if(_.undefined(toolbarContainer)) {
          toolbarContainer = containerElement;
        }
        var toolbarElement = toolbarContainer.append('div').attr('id', 'topology-control-button').attr('class', 'btn-toolbar pull-right').attr('role',
         'toolbar').append('div').attr('class', 'btn-group');
        this.svg = containerElement.append('svg:svg');
        this.controls = svgControlsFactory.create(this.svg, toolbarElement, graphControl);
        this.svg.attr('id', svgId);
        this.svg.attr('class', svgClass);
        this.svgGroup = this.controls.svgGroup;
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
          this.svg.attr('width', width);
          this.svg.attr('height', height);
          this.controls.resize(width, height);
        }
      };

      return {
        create: function(containerElement, svgId, svgClass, toolbarContainer, graphControl) {
          return new SvgGraph(containerElement, svgId, svgClass, toolbarContainer, graphControl);
        }
      };
    } // function
  ]); // factory
}); // define
