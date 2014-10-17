/* global UTILS */

'use strict';

angular.module('alienUiApp').factory('simpleNodeRendererService', [
  function() {
    return {
      width: 50,
      height: 70,
      distanceBetweenBranchHorizontal: 50,
      distanceBetweenNodeHorizontal: 30,
      distanceBetweenNodeVertical: 40,

      createNode: function (nodeGroup, node, nodeTemplate, nodeType, oX, oY) {
        if (nodeType.tags) {
          var tags = UTILS.convertNameValueListToMap(nodeType.tags);
          if (tags.icon) {
            nodeGroup.append('image').attr('x', oX + 10).attr('y', oY + 8).attr('width', '32').attr('height', '32').attr('xlink:href',
            'img?id=' + tags.icon + '&quality=QUALITY_32');
          }
        }

        nodeGroup.append('text').attr('text-anchor', 'start').attr('class', 'simple-title small').attr('x', oX + 2).attr('y', oY + 52);
        nodeGroup.append('text').attr('text-anchor', 'start').attr('class', 'simple-version small').attr('x', oX + 2).attr('y', oY + 66);
      },

      updateNode: function(nodeGroup, node, nodeTemplate, nodeType, oX, oY) {
        // update text
        nodeGroup.select('.simple-title').text(this.getDisplayId(node));

        // update version
        nodeGroup.select('.simple-version').text(function() {
          if (nodeTemplate.properties) {
            if (typeof nodeTemplate.properties.version === 'string') {
              return 'v' + nodeTemplate.properties.version;
            }
          }
        });
      },

      getDisplayId: function(node) {
        var nodeTemplateNameSizeCut = 2;
        if (node.id.length >= 8) {
          return node.id.substring(0, 8 - nodeTemplateNameSizeCut) + '...';
        } else {
          return node.id;
        }
      },

      tooltip: function(node, nodeTemplate, nodeType) {
        var tooltipContent = '<div>';

        tooltipContent += '<div>'+node.id;
        if (nodeType.abstract) {
          var icoSize = 16;
          tooltipContent += ' <img src="images/abstract_ico.png" height="'+icoSize+'" width="'+icoSize+'"></img>';
        }
        tooltipContent += '</div>';

        if (nodeTemplate.properties) {
          if (typeof nodeTemplate.properties.version === 'string') {
            tooltipContent += '<div>'+'v' + nodeTemplate.properties.version+'</div>';
          }
        }

        // specific to the runtime view
        // if (this.isRuntime) {
        //   nodeGroup.append('g').attr('id', 'runtime');
        // }
        tooltipContent += '</div>';
        return tooltipContent;
      }
    };
  } // function
]);
