define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/controllers/orchestrator_resource_template');
  require('scripts/orchestrators/directives/orchestrator_resource_template');
  require('scripts/orchestrators/services/location_resources_processor');

  states.state('admin.orchestrators.details.locations.infra', {
    url: '/infra',
    templateUrl: 'views/orchestrators/orchestrator_locations_infra.html',
    controller: 'OrchestratorLocationsConfigCtrl',
    menu: {
      id: 'menu.orchestrators.locations.infra',
      state: 'admin.orchestrators.details.locations.infra',
      key: 'ORCHESTRATORS.LOCATIONS.CONFIGURATION_RESOURCES',
      icon: 'fa fa-wrench',
      priority: 100
    }
  });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('OrchestratorLocationsConfigCtrl',
    ['$scope', 'orchestrator', 'locationResourcesService', 'locationResourcesProcessor',
      function($scope, orchestrator, locationResourcesService, locationResourcesProcessor) {
        $scope.orchestrator = orchestrator;
        if (_.isNotEmpty($scope.context.configurationTypes)) {
          $scope.selectedConfigurationResourceType = $scope.context.configurationTypes[0];
        }
        $scope.addResourceTemplate = function() {
          locationResourcesService.save({
            orchestratorId: $scope.orchestrator.id,
            locationId: $scope.context.location.id
          }, angular.toJson({
            'resourceType': $scope.selectedConfigurationResourceType.elementId,
            'resourceName': 'New Resource'
          }), function(response) {
            locationResourcesProcessor.processLocationResourceTemplate(response.data)
            $scope.context.locationResources.configurationTemplates.push(response.data);
            $scope.selectTemplate(response.data);
          });
        };

        $scope.selectTemplate = function(template) {
          $scope.selectedConfigurationResourceTemplate = template;
        };

        $scope.saveResourceTemplate = function(resourceTemplate) {
          console.log('Update', resourceTemplate);
        };

        $scope.deleteResourceTemplate = function(resourceTemplate) {
          console.log('Delete', resourceTemplate);
          locationResourcesService.delete({
            orchestratorId: $scope.orchestrator.id,
            locationId: $scope.context.location.id,
            id: resourceTemplate.id
          }, undefined, function() {
            _.remove($scope.context.locationResources.configurationTemplates, {
              id: resourceTemplate.id
            });
            delete $scope.selectedConfigurationResourceTemplate;
          });
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
