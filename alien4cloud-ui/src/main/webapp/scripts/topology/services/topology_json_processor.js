define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/list_to_map_service');

  modules.get('a4c-common').factory('topologyJsonProcessor', ['listToMapService', function(listToMapService) {
      // This service post-process a topology json in order to add maps field from ordered maps array (array of MapEntry).
      return {
        /**
         * Compute the number of times that a requirement template is used by relationships.
         *
         * @param topology The topology dto object to post-process after json deserialization.
         */
        process: function(topology) {
          listToMapService.processMap(topology.nodeTypes, 'properties');
          listToMapService.processMap(topology.policyTypes, 'properties');
          listToMapService.processMap(topology.relationshipTypes, 'properties');
          listToMapService.processMap(topology.capabilityTypes, 'properties');

          this.postProcess(topology.topology.nodeTemplates, ['properties', 'attributes', 'requirements', 'relationships', 'capabilities']);
          for (var nodeTemplateName in topology.topology.nodeTemplates) {
            if (topology.topology.nodeTemplates.hasOwnProperty(nodeTemplateName)) {
              if (_.defined(topology.topology.nodeTemplates[nodeTemplateName].relationships)) {
                for (var i = 0; i < topology.topology.nodeTemplates[nodeTemplateName].relationships.length; i++) {
                  listToMapService.process(topology.topology.nodeTemplates[nodeTemplateName].relationships[i].value, 'properties');
                }
              }
              if (_.defined(topology.topology.nodeTemplates[nodeTemplateName].capabilities)) {
                for (var j = 0; j < topology.topology.nodeTemplates[nodeTemplateName].capabilities.length; j++) {
                  listToMapService.process(topology.topology.nodeTemplates[nodeTemplateName].capabilities[j].value, 'properties');
                }
              }
            }
          }
        },

        postProcess: function(object, properties) {
          for (var i = 0; i < properties.length; i++) {
            listToMapService.processMap(object, properties[i]);
          }
        }
      };
    } // function
  ]); // factory
}); // define
