/* global UTILS, $ */

'use strict';

var D3JS_UTILS = {
  rect: function(d3AppendGroup, x, y, width, height, rx, ry) {
    return d3AppendGroup.append('rect').attr('x', x).attr('y', y).attr('width', width).attr('height', height).attr('rx', rx).attr('ry', ry);
  },

  rectWithTooltip: function(d3AppendGroup, x, y, width, height, rx, ry, tooltipText) {
    d3AppendGroup = this.rect(d3AppendGroup, x, y, width, height, rx, ry);
    if (tooltipText !== null) {
      // split the name to fit the default tooltip size
      if (tooltipText.length >= UTILS.maxNodeNameDrawSize) {
        tooltipText = UTILS.splitString(tooltipText, UTILS.maxNodeNameDrawSize);
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
