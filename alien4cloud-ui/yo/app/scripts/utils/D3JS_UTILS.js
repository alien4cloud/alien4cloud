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
  },
  
  string_to_color: function(str, prc) {
    // https://github.com/brandoncorbin/string_to_color
    
    // Check for optional lightness/darkness
    var prc = typeof prc === 'number' ? prc : -10;

    // Generate a Hash for the String
    var hash = function(word) {
        var h = 0;
        for (var i = 0; i < word.length; i++) {
            var charCode = word.charCodeAt(i);
            // just to ensure that the hash for quite similar names will be different (ex 'Group A', 'Group B' ...)
            charCode = charCode << (charCode % ((charCode % 2 == 0) ? 16 : 6));
            h = charCode + ((h << 5) - h);
        }
        return h;
    };

    // Change the darkness or lightness
    var shade = function(color, prc) {
        var num = parseInt(color, 16),
            amt = Math.round(2.55 * prc),
            R = (num >> 16) + amt,
            G = (num >> 8 & 0x00FF) + amt,
            B = (num & 0x0000FF) + amt;
        return (0x1000000 + (R < 255 ? R < 1 ? 0 : R : 255) * 0x10000 +
            (G < 255 ? G < 1 ? 0 : G : 255) * 0x100 +
            (B < 255 ? B < 1 ? 0 : B : 255))
            .toString(16)
            .slice(1);
    };

    // Convert init to an RGBA
    var int_to_rgba = function(i) {
        var color = ((i >> 24) & 0xFF).toString(16) +
            ((i >> 16) & 0xFF).toString(16) +
            ((i >> 8) & 0xFF).toString(16) +
            (i & 0xFF).toString(16);
        return color;
    };

    return shade(int_to_rgba(hash(str)), prc);
  }
  
};
