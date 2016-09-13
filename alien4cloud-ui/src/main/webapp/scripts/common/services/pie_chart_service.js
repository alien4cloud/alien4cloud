define(function (require) {
  'use strict';

  var modules = require('modules');

  var d3Tip = require('d3-tip');
  var d3 = require('d3');
  window.d3 = d3;
  var d3pie = require('d3-pie');
  var $ = require('jquery');

  modules.get('a4c-common').factory('pieChartService', function () {
    return {
      render: function (appId, data) {
        // Empty old content if any before rendering new content
        $('#pieChart-' + appId).empty();
        if (data.length > 0) {
          var tip = d3Tip().attr('class', 'd3-tip').html(function (node) {
            return node.data.name;
          });

          // Ignored: A constructor name should start with an uppercase letter.
          var pie = new d3pie('pieChart-' + appId, { // jshint ignore:line
            'size': {
              'canvasWidth': 60,
              'canvasHeight': 60
            },
            'data': {
              'sortOrder': 'label-asc',
              'content': data
            },
            'labels': {
              'outer': {
                'format': 'none'
              },
              'inner': {
                'format': 'none'
              }
            },
            'effects': {
              'load': {
                'effect': 'none'
              },
              'pullOutSegmentOnClick': {
                'effect': tip.hide
              },
              'highlightSegmentOnMouseover': true,
              'highlightLuminosity': 0.10
            },
            'callbacks': {
              'onMouseoverSegment': function (data) {
                tip.show(data);
              },
              'onMouseoutSegment': tip.hide
            }
          });

          pie.svg.call(tip);
        }
      }
    };
  });
});
