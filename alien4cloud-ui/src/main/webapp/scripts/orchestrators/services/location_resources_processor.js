define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/list_to_map_service');

  modules.get('a4c-orchestrators').factory('locationResourcesProcessor', ['listToMapService', function(listToMapService) {
    // This service post-process an orchestrator json in order to add maps field from ordered maps array (array of MapEntry).
    return {

      process: function(resources) {
        listToMapService.processMap(resources.nodeTypes, 'properties');
        listToMapService.processMap(resources.configurationTypes, 'properties');
        listToMapService.processMap(resources.capabilityTypes, 'properties');
        this.processNodeTemplates(resources.nodeTemplates);
        this.processNodeTemplates(resources.configurationTemplates);
      },

      processNodeTemplates: function(locationResourceTemplates) {
        for (var i = 0; i < locationResourceTemplates.length; i++) {
          listToMapService.process(locationResourceTemplates[i].template, 'properties');
          listToMapService.process(locationResourceTemplates[i].template, 'requirements');
          listToMapService.process(locationResourceTemplates[i].template, 'capabilities');
          for (var nodeTemplateName in locationResourceTemplates[i]) {
            if (locationResourceTemplates[i].hasOwnProperty(nodeTemplateName)) {
              this.processNodeCapability(locationResourceTemplates[i][nodeTemplateName]);
            }
          }
        }
      },

      processNodeCapability: function(nodeTemplate) {
        if (_.defined(nodeTemplate.capabilities)) {
          for (var j = 0; j < nodeTemplate.capabilities.length; j++) {
            listToMapService.process(nodeTemplate.capabilities[j].value, 'properties');
          }
        }
      }
    };
  } // function
  ]); // factory
}); // define
