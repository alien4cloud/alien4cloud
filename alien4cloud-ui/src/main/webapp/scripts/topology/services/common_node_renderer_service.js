// Directive allowing to display a workflow (tosca plan)
define(function (require) {
  'use strict';

  var modules = require('modules');

  var _ = require('lodash');

  modules.get('a4c-topology-editor').factory('commonNodeRendererService', [ 'd3Service',
    function(d3Service) {
      return {
        //----------------------
        // Services
        //----------------------
        tooltip: function(element) {
          var tooltipContent = '<div>';

          tooltipContent += '<div>' + element.id;
          if (_.defined(element.type) && element.type.abstract) {
            var icoSize = 16;
            tooltipContent += ' <img src="images/abstract_ico.png" height="' + icoSize + '" width="' + icoSize + '"></img>';
          }
          tooltipContent += '</div>';
          if (_.defined(element.template.properties)) {
            if (typeof element.template.properties.version === 'string') {
              tooltipContent += '<div>' + 'v' + element.template.properties.version + '</div>';
            }
          }
          tooltipContent += '</div>';
          return tooltipContent;
        },

        getDisplayId: function(node, simple) {
          var nodeTemplateNameSizeCut = 2;
          if (node.id.length >= d3Service.tooltipTextLenghtTrigger || simple) {
            nodeTemplateNameSizeCut = simple === true ? 9 : nodeTemplateNameSizeCut;
            return node.id.substring(0, d3Service.tooltipTextLenghtTrigger - nodeTemplateNameSizeCut) + '...';
          } else {
            return node.id;
          }
        },

        removeRuntimeCount: function(runtimeGroup, id) {
          var groupSelection = runtimeGroup.select('#' + id);
          if (!groupSelection.empty()) {
            groupSelection.remove();
          }
        },

        getNumberOfInstanceByStatus: function(nodeInstances, instanceStatus, state) {
          var count = 0;
          for (var instanceId in nodeInstances) {
            if (nodeInstances.hasOwnProperty(instanceId)) {
              if (nodeInstances[instanceId].instanceStatus === instanceStatus || nodeInstances[instanceId].state === state) {
                count++;
              }
            }
          }
          return count;
        }
      }; // end returned service
    } // function
  ]); // factory
}); // define
