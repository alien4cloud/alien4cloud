define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/controllers/orchestrator_location_resource_template');
  require('scripts/orchestrators/directives/orchestrator_location_resource_template');
  require('scripts/orchestrators/services/location_resources_processor');


  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('OrchestratorLocationResourcesTemplateCtrl',
    ['$scope', 'locationResourcesService', 'locationResourcesProcessor',
      function($scope, locationResourcesService, locationResourcesProcessor) {
        if (_.isNotEmpty($scope.resourcesTypes)) {
          $scope.selectedConfigurationResourceType = $scope.resourcesTypes[0];
        }
        $scope.addResourceTemplate = function() {
          locationResourcesService.save({
            orchestratorId: $scope.context.orchestrator.id,
            locationId: $scope.context.location.id
          }, angular.toJson({
            'resourceType': $scope.selectedConfigurationResourceType.elementId,
            'resourceName': 'New Resource'
          }), function(response) {
            locationResourcesProcessor.processLocationResourceTemplate(response.data)
            $scope.resourcesTemplates.push(response.data);
            $scope.selectTemplate(response.data);
          });
        };

        // delete is called from the directive but must be managed here as we must delete the selectedResourceTemplate on success
        $scope.deleteResourceTemplate = function(resourceTemplate) {
          locationResourcesService.delete({
            orchestratorId: $scope.context.orchestrator.id,
            locationId: $scope.context.location.id,
            id: resourceTemplate.id
          }, undefined, function() {
            _.remove($scope.resourcesTemplates, {
              id: resourceTemplate.id
            });
            delete $scope.selectedResourceTemplate;
          });
        };

        $scope.selectTemplate = function(template) {
          $scope.selectedResourceTemplate = template;
        };

        $scope.getIcon = function(template) {
          var templateType = $scope.context.locationResources.configurationTypes[template.template.type];
          if (_.isNotEmpty(templateType) && _.isNotEmpty(templateType.tags)) {
            var icons = _.find(templateType.tags, {'name': 'icon'});
            if (_.isNotEmpty(icons)) {
              return icons.value;
            }
          }
        }
      }
    ]); // controller
}); // define
