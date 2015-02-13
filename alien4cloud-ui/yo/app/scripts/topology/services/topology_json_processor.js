/* global UTILS */
'use strict';

angular.module('alienUiApp').factory('topologyJsonProcessor',
  ['orderedMapEnricher', function(orderedMapEnricher) {
    // This service post-process a topology json in order to add maps field from ordered maps array (array of MapEntry).
    return {
      /**
      * Compute the number of times that a requirement template is used by relationships.
      *
      * @param topology The topology dto object to post-process after json deserialization.
      */
      process: function(topology) {
        orderedMapEnricher.processMap(topology.nodeTypes, 'properties');
        this.postProcess(topology.topology.nodeTemplates, ['properties', 'attributes', 'requirements', 'relationships', 'capabilities']);
      },

      postProcess: function(object, properties) {
        for(var i =0;i<properties.length;i++) {
          orderedMapEnricher.processMap(object, properties[i]);
        }
      }
    };
  } // function
]);
