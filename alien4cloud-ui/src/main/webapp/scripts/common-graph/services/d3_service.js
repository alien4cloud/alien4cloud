// Service to ease d3 manipulation
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common-graph').factory('d3Service', function() {
    return {
      tooltipTextLenghtTrigger: 17,

      // Performs a d3 selection from the given element
      select: function(from, collection, selector, processor, collectionIdentifier) {
        if(_.undefined(collectionIdentifier)) { // default identifier is id based.
          collectionIdentifier = function(element) { return element.id };
        }
        var selection = from.selectAll(selector).data(collection, collectionIdentifier);
        // update existing nodes
        selection.each(function(element) {
          var group = d3.select(this);
          processor.update(group, element);
        });

        // create new nodes
        var newGroups = processor.enter(selection.enter());
        newGroups.each(function(element) {
          var group = d3.select(this);
          processor.create(group, element);
          processor.update(group, element);
        });

        // remove destroyed nodes.
        selection.exit().remove();
      },

      circle: function(d3AppendGroup, cx, cy, r) {
        return d3AppendGroup.append("circle").attr("cx", cx).attr("cy", cy).attr("r", r);
      },

      rect: function(d3AppendGroup, x, y, width, height, rx, ry) {
        if(_.undefined(rx)) {
          rx = 0;
        }
        if(_.undefined(ry)) {
          ry = 0;
        }
        return d3AppendGroup.append('rect').attr('x', x).attr('y', y).attr('width', width).attr('height', height).attr('rx', rx).attr('ry', ry);
      },

      rectWithTooltip: function(d3AppendGroup, x, y, width, height, rx, ry, tooltipText) {
        d3AppendGroup = this.rect(d3AppendGroup, x, y, width, height, rx, ry);
        if (tooltipText !== null) {
          // split the name to fit the default tooltip size
          if (tooltipText.length >= this.tooltipTextLenghtTrigger) {
            tooltipText = _.splitString(tooltipText, this.tooltipTextLenghtTrigger);
            d3AppendGroup.attr('title', tooltipText.join('\n'));
          }
        }

        // applying the tooltip for all <rect> tag (default top display)
        $(document).ready(function() {
          $('rect').tooltip({
            'container': 'body',
            'placement': 'top'
          });
        });

        // remove the tooltip
        $(document).on('hidden.bs.tooltip', function() {
          var tooltips = $('.tooltip').not('.in');
          if (tooltips) {
            tooltips.remove();
          }
        });

        return d3AppendGroup;
      }
    };
  }); // factory
}); // define
