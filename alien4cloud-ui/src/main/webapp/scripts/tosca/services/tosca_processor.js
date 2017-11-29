/**
* This service is used to process TOSCA node types and node templates to create properties map and properties
*/
define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/list_to_map_service');

  modules.get('a4c-orchestrators').factory('a4cToscaProcessor', ['listToMapService', function(listToMapService) {
    function processObject(toProcess, propertyToProcess){
      // if the type has not been processed yet
      if(_.definedPath(toProcess, propertyToProcess) && _.undefinedPath(toProcess, propertyToProcess+'Map')) {
        listToMapService.process(toProcess, propertyToProcess);
      }
    }
    return {
      processInheritableToscaTypes: function(toscaTypes){
        var _this = this;
        _.forEach(toscaTypes, function(toscaType){_this.processInheritableToscaType(toscaType);});

      },
      processInheritableToscaType: function(toscaType){
        processObject(toscaType, 'properties');
      },
      processTemplate: function(template) {
        processObject(template, 'properties');
      },
      processNodeTemplate: function(nodeTemplate) {
        processObject(nodeTemplate, 'properties');
        processObject(nodeTemplate, 'requirements');
        processObject(nodeTemplate, 'capabilities');
        _.each(nodeTemplate.capabilitiesMap, function(capabilityEntry){
          processObject(capabilityEntry.value, 'properties');
        });
      }
    };
  } // function
  ]); // factory
}); // define
