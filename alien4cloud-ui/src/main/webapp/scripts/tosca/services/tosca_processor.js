/**
* This service is used to process TOSCA node types and node templates to create properties map and properties
*/
define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/list_to_map_service');

  modules.get('a4c-orchestrators').factory('a4cToscaProcessor', ['listToMapService', function(listToMapService) {
    return {
      processNodeType: function(nodeType) {
        // if the node has not been processed yet
        if(_.defined(nodeType.properties) && _.undefined(nodeType.propertiesMap)) {
          listToMapService.process(nodeType, 'properties');
        }
      },

      processNodeTemplate: function(nodeTemplate) {
        // if the node has not been processed yet
        if(_.defined(nodeTemplate.properties) && _.undefined(nodeTemplate.propertiesMap)) {
          listToMapService.process(nodeTemplate, 'properties');
          listToMapService.process(nodeTemplate, 'requirements');
          listToMapService.process(nodeTemplate, 'capabilities');
          _.each(nodeTemplate.capabilitiesMap, function(capabilityEntry){
            listToMapService.process(capabilityEntry.value, 'properties');
          });
        }
      },

      processCapabilityTypes: function(capabilityTypes) {
        var _this = this;
        _.each(capabilityTypes, function(capabilityType){ _this.processCapabilityType(capabilityType); });
      },

      processCapabilityType: function(capabilityType) {
        if(_.defined(capabilityType.properties) && _.undefined(capabilityType.propertiesMap)) {
          listToMapService.process(capabilityType, 'properties');
        }
      }
    };
  } // function
  ]); // factory
}); // define
