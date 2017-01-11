define(function(require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/controllers/orchestrator_location_resource_template');
  require('scripts/orchestrators/directives/orchestrator_location_resource_template');
  require('scripts/orchestrators/services/location_resources_processor');
  require('scripts/tosca/services/node_template_service');
  require('scripts/common/services/resize_services');

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('OrchestratorLocationResourcesTemplateCtrl',
    ['$scope', 'locationResourcesService', 'locationResourcesPropertyService', 'locationResourcesCapabilityPropertyService', 'locationResourcesProcessor', 'nodeTemplateService', 'locationResourcesPortabilityService', 'resizeServices',
      function($scope, locationResourcesService, locationResourcesPropertyService, locationResourcesCapabilityPropertyService, locationResourcesProcessor, nodeTemplateService, locationResourcesPortabilityService, resizeServices) {

        var init = function(){
          if (_.isNotEmpty($scope.resourcesTypes)) {
            $scope.selectedConfigurationResourceType = $scope.resourcesTypes[0];
          }
          if ($scope.showCatalog) {
            $scope.dimensions = { width: 800, height: 600 }; // TODO GET HOW THIS WORKS ??
            resizeServices.registerContainer(function (width, height) {
              $scope.dimensions = { width: width, height: height };
              $scope.$digest();
            }, "#catalog");
          }
        };

        $scope.$watch('resourcesTypes', function(){
          init();
        });

        $scope.addResourceTemplate = function() {
          locationResourcesService.save({
            orchestratorId: $scope.context.orchestrator.id,
            locationId: $scope.context.location.id
          }, angular.toJson({
            'resourceType': $scope.selectedConfigurationResourceType.elementId,
            'resourceName': 'New Resource'
          }), function(response) {
            locationResourcesProcessor.processLocationResourceTemplate(response.data);
            $scope.resourcesTemplates.push(response.data);
            $scope.selectTemplate(response.data);
          });
        };

        // delete is called from the directive but must be managed here as we must delete the selectedResourceTemplate on success
        $scope.deleteResourceTemplate = function(resourceTemplate) {
          locationResourcesService.delete({
            orchestratorId: $scope.context.orchestrator.id,
            locationId: $scope.context.location.id,
            id: $scope.selectedResourceTemplate.id
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
          var templateType = $scope.resourcesTypesMap[template.template.type];
          return nodeTemplateService.getNodeTypeIcon(templateType);
        };


        $scope.updateLocationResource = function(propertyName, propertyValue) {
          var updateLocationRequest = {};
          updateLocationRequest[propertyName] = propertyValue;
          return locationResourcesService.update({
            orchestratorId: $scope.context.orchestrator.id,
            locationId: $scope.context.location.id,
            id: $scope.selectedResourceTemplate.id
          }, angular.toJson(updateLocationRequest)).$promise;
        };

        $scope.updateResourceProperty = function(propertyName, propertyValue) {
          return locationResourcesPropertyService.save({
            orchestratorId: $scope.context.orchestrator.id,
            locationId: $scope.context.location.id,
            id: $scope.selectedResourceTemplate.id
          }, angular.toJson({
            propertyName: propertyName,
            propertyValue: propertyValue
          })).$promise;
        };

        $scope.updateResourceCapabilityProperty = function(capabilityName, propertyName, propertyValue) {
          return locationResourcesCapabilityPropertyService.save({
            orchestratorId: $scope.context.orchestrator.id,
            locationId: $scope.context.location.id,
            id: $scope.selectedResourceTemplate.id,
            capabilityName: capabilityName
          }, angular.toJson({
            propertyName: propertyName,
            propertyValue: propertyValue
          })).$promise;
        };

        $scope.updatePortabilityProperty = function(propertyName, propertyValue) {
          return locationResourcesPortabilityService.save({
            orchestratorId: $scope.context.orchestrator.id,
            locationId: $scope.context.location.id,
            id: $scope.selectedResourceTemplate.id
          }, angular.toJson({
            propertyName: propertyName,
            propertyValue: propertyValue
          })).$promise;
        };
        
        $scope.isPropertyEditable = function() {
          return true;
        };
      }
    ]);
});
