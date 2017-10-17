define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/list_to_map_service');

  modules.get('a4c-orchestrators').factory('locationResourcesProcessor', ['listToMapService', function(listToMapService) {
    // This service post-process an orchestrator json in order to add maps field from ordered maps array (array of MapEntry).
    function processObject(toProcess, propertyToProcess){
      // if the type has not been processed yet
      if(_.definedPath(toProcess, propertyToProcess) && _.undefinedPath(toProcess, propertyToProcess+'Map')) {
        listToMapService.process(toProcess, propertyToProcess);
      }
    }

    return {

      processLocationResources: function(resources) {
        listToMapService.processMap(resources.nodeTypes, 'properties');
        listToMapService.processMap(resources.configurationTypes, 'properties');
        listToMapService.processMap(resources.capabilityTypes, 'properties');
        listToMapService.processMap(resources.dataTypes, 'properties');
        listToMapService.processMap(resources.policyTypes, 'properties');
        this.processLocationResourceTemplates(resources.nodeTemplates);
        this.processLocationResourceTemplates(resources.configurationTemplates);
        this.processTemplates(resources.policyTemplates);
      },
      /*process the template 'properties' memeber. Usually all location resources template have this propety in common*/
      processTemplates: function(templates){
        var _this = this;
        _.forEach(templates, function(template){
          _this.processTemplate(template);
        });
      },
      processTemplate: function(template) {
        processObject(template.template, 'properties');
      },
      processLocationResourceTemplates: function(locationResourceTemplates) {
        for (var i = 0; i < locationResourceTemplates.length; i++) {
          this.processLocationResourceTemplate(locationResourceTemplates[i]);
        }
      },
      processLocationResourceTemplatesMap: function(locationResourceTemplates) {
        for (var nodeId in locationResourceTemplates) {
          if (locationResourceTemplates.hasOwnProperty(nodeId)) {
            this.processLocationResourceTemplate(locationResourceTemplates[nodeId]);
          }
        }
      },
      processLocationResourceTemplate: function(locationResourceTemplate) {
        listToMapService.process(locationResourceTemplate.template, 'properties');
        listToMapService.process(locationResourceTemplate.template, 'requirements');
        listToMapService.process(locationResourceTemplate.template, 'capabilities');
        this.processNodeRequirementCapability(locationResourceTemplate.template, 'capabilities');
        this.processNodeRequirementCapability(locationResourceTemplate.template, 'requirements');
      },
      processNodeRequirementCapability: function(nodeTemplate, propertyName) {
        if (_.definedPath(nodeTemplate, propertyName)) {
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
