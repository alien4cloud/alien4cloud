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
        this.processLocationResourceTemplates(resources.nodeTemplates);
        this.processLocationResourceTemplates(resources.configurationTemplates);
      },

      processLocationResourceTemplates: function(locationResourceTemplates) {
        for (var i = 0; i < locationResourceTemplates.length; i++) {
          this.processLocationResourceTemplate(locationResourceTemplates[i]);
        }
      },

      processLocationResourceTemplate: function(locationResourceTemplate) {
        listToMapService.process(locationResourceTemplate.template, 'properties');
        listToMapService.process(locationResourceTemplate.template, 'requirements');
        listToMapService.process(locationResourceTemplate.template, 'capabilities');
        for (var nodeTemplateName in locationResourceTemplate) {
          if (locationResourceTemplate.hasOwnProperty(nodeTemplateName)) {
            this.processNodeRequirementCapability(locationResourceTemplate[nodeTemplateName], 'capabilities');
            this.processNodeRequirementCapability(locationResourceTemplate[nodeTemplateName], 'requirements');
          }
        }
      },

      processNodeRequirementCapability: function(nodeTemplate, propertyName) {
        if (_.defined(nodeTemplate[propertyName])) {
          for (var j = nodeTemplate[propertyName].length - 1; j >= 0; j--) {
            if (_.isNotEmpty(nodeTemplate[propertyName][j].value.properties)) {
              listToMapService.process(nodeTemplate[propertyName][j].value, 'properties');
            } else {
              nodeTemplate[propertyName].splice(j, 1);
            }
          }
        }
      }
    };
  } // function
  ]); // factory
}); // define
