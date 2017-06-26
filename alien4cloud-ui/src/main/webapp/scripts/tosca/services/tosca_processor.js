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
        if(_.definedPath(nodeType,'properties') && _.undefinedPath(nodeType,'propertiesMap')) {
          listToMapService.process(nodeType, 'properties');
        }
      },
      processNodeTemplate: function(nodeTemplate) {
        // if the node has not been processed yet
        if(_.definedPath(nodeTemplate,'properties') && _.undefinedPath(nodeTemplate,'propertiesMap')) {
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
      processDataTypes: function(dataType) {
        // if the node has not been processed yet
        if(_.definedPath(dataType,'properties') && _.undefinedPath(dataType,'propertiesMap')) {
          listToMapService.process(dataType, 'properties');
        }
      },
      processCapabilityType: function(capabilityType) {
        if(_.definedPath(capabilityType,'properties') && _.undefinedPath(capabilityType,'propertiesMap')) {
          listToMapService.process(capabilityType, 'properties');
        }
      }
    };
  } // function
  ]); // factory
}); // define
