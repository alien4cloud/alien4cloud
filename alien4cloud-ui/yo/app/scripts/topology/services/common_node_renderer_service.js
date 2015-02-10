/* global UTILS */

'use strict';

angular.module('alienUiApp').factory('commonNodeRendererService', [
  function() {
    return {

      getDisplayId: function(node, simple) {
        var nodeTemplateNameSizeCut = 2;
        if (node.id.length >= UTILS.maxNodeNameDrawSize || simple) {
          nodeTemplateNameSizeCut = simple === true ? 9 : nodeTemplateNameSizeCut;
          return node.id.substring(0, UTILS.maxNodeNameDrawSize - nodeTemplateNameSizeCut) + '...';
        } else {
          return node.id;
        }
      },

      createNode: function(nodeGroup, node, nodeTemplate, nodeType, oX, oY, simple) {
        if (nodeType.tags) {
          var tags = UTILS.convertNameValueListToMap(nodeType.tags);
          if (tags.icon) {
            nodeGroup.append('image').attr('x', oX + 8).attr('y', oY + 8).attr('width', '32').attr('height', '32').attr('xlink:href',
              'img?id=' + tags.icon + '&quality=QUALITY_32');
          }
        }

        if (nodeType.abstract && !simple) {
          var icoSize = 16;
          var x = 80;
          var y = -22;
          nodeGroup.append('image').attr('x', x).attr('y', y).attr('width', icoSize).attr('height', icoSize).attr('xlink:href', 'images/abstract_ico.png');
        }

        nodeGroup.append('text').attr('text-anchor', 'start').attr('class', 'title').attr('x', oX + 44).attr('y', oY + 20);
        nodeGroup.append('text').attr('text-anchor', 'end').attr('class', 'version').attr('x', '80').attr('y', '20');

        // specific to the runtime view
        if (this.isRuntime) {
          nodeGroup.append('g').attr('id', 'runtime');
        }
      },

      drawRuntimeCount: function(runtimeGroup, id, rectOriginX, currentY, iconCode, count, instanceCount) {
        var groupSelection = runtimeGroup.select('#' + id);
        // improve that...
        var counter = (count || '?') + '/' + (instanceCount || '?');
        if (groupSelection.empty()) {
          groupSelection = runtimeGroup.append('g').attr('id', id);
          groupSelection.append('text').attr('class', 'topology-svg-icon').attr('text-anchor', 'start').attr('x', rectOriginX + 60).attr('y', currentY).text(iconCode);
          groupSelection.append('text').attr('id', 'count-text').attr('text-anchor', 'start').attr('x', rectOriginX + 80).attr('y', currentY).text(counter);
        } else {
          groupSelection.select('#count-text').text(counter);
        }
      },

      tooltip: function(node, nodeTemplate, nodeType) {
        var tooltipContent = '<div>';

        tooltipContent += '<div>' + node.id;
        if (nodeType.abstract) {
          var icoSize = 16;
          tooltipContent += ' <img src="images/abstract_ico.png" height="' + icoSize + '" width="' + icoSize + '"></img>';
        }
        tooltipContent += '</div>';
        if (nodeTemplate.properties) {
          if (typeof nodeTemplate.properties.version === 'string') {
            tooltipContent += '<div>' + 'v' + nodeTemplate.properties.version + '</div>';
          }
        }
        tooltipContent += '</div>';
        return tooltipContent;
      }

    };
  } // function
]);
