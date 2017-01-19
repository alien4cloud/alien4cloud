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
  require('scripts/components/services/component_services');

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('OrchestratorLocationResourcesTemplateCtrl',
    ['$scope', 'locationResourcesService', 'locationResourcesPropertyService', 'locationResourcesCapabilityPropertyService',
      'locationResourcesProcessor', 'nodeTemplateService', 'locationResourcesPortabilityService', 'resizeServices', 'componentService',
      function($scope, locationResourcesService, locationResourcesPropertyService,
               locationResourcesCapabilityPropertyService, locationResourcesProcessor, nodeTemplateService,
               locationResourcesPortabilityService, resizeServices, componentService) {
        const vm = this;
        var init = function(){
          if (_.isNotEmpty($scope.resourcesTypes)) {
            $scope.selectedConfigurationResourceType = $scope.resourcesTypes[0];
          }
          // Only show catalog on custom on-demand resources tab
          if (!$scope.showCatalog) return;

          $scope.dimensions = { width: 800, height: 600 }; // TODO GET HOW THIS WORKS ??
          resizeServices.registerContainer(function (width, height) {
            $scope.dimensions = { width: width, height: height };
            $scope.$digest();
          }, "#catalog");

          console.table($scope.resourcesTypes);
          // join favorites types with types from the orchestrator definition
          vm.favorites = computeTypes();
        };

        $scope.$watch('resourcesTypes', function(){
          init();
        });

        $scope.addResourceTemplate = function(dragData) {
          const source = dragData ? angular.fromJson(dragData.source) : $scope.selectedConfigurationResourceType;
          if (!source) return;
          const body = {
            'resourceType': source.elementId,
            'resourceName': 'New resource',
            'archiveName': source.archiveName || '',
            'archiveVersion': source.archiveVersion || '',
            'id': source.id
          };

          locationResourcesService.save({
            orchestratorId: $scope.context.orchestrator.id,
            locationId: $scope.context.location.id
          }, angular.toJson(body), function(response) {
            const resourceTemplate = response.data.resourceTemplate;
            const updatedDependencies = response.data.newDependencies;

            $scope.resourcesTemplates.push(resourceTemplate);
            $scope.context.location.dependencies = updatedDependencies;

            locationResourcesProcessor.processLocationResourceTemplate(resourceTemplate);

            if ($scope.showCatalog && body.id && _.findIndex(vm.favorites, 'id', body.id) == -1) {
              // ResourceType was not in the fav list - get its type and add it to resource types map
              const typeId = body.resourceType;
              const componentId = body.id;

              componentService.get({componentId: componentId}).$promise.then(function (res) {
                const resourceType = res.data;
                const promises = [];
                _.forEach(resourceType.capabilities, function(capability) {
                  if (!_.has($scope.context.locationResources.capabilityTypes, capability.type)) {
                  // Query capability type if needed using getInArchives - passing the location dependencies
                    const p = componentService.getInArchives(capability.type, 'CAPABILITY_TYPE', updatedDependencies)
                      .then(function (res) {
                        const capabilityType = res.data.data;
                        capabilityType['propertiesMap'] = _.indexBy(capabilityType.properties, 'key');
                        $scope.context.locationResources.capabilityTypes[capability.type] = capabilityType;
                      });
                    promises.push(p);
                  }
                });

                // keep track of promises to wait before selecting the template. Use $apply to make sure watches are triggered.
                Promise.all(promises).then(function () {
                  $scope.$apply(function() {
                    // Compute properties map and select the template.
                    resourceType['propertiesMap'] = _.indexBy(resourceType.properties, 'key');
                    $scope.resourcesTypesMap[typeId] = resourceType;
                    $scope.resourcesTypes.push(resourceType);
                    $scope.selectedResourceTemplate = resourceTemplate;
                  });
                });
              });

              body.elementId = body.resourceType;
              delete body.resourceType;
              vm.favorites.push(body);
            } else {
              $scope.selectTemplate(resourceTemplate);
            }
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
            vm.favorites = computeTypes();
          });
        };

        $scope.selectTemplate = function(template) {
          $scope.selectedResourceTemplate = template;
        };

        $scope.getIcon = function(resourceType) {
          var templateType = $scope.resourcesTypesMap[resourceType];
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

        function computeTypes() {
          // pick all resource types from the location - this will include orchestrator & custom types
          const resourcesTypes = _.map($scope.resourcesTypes, function (res) {
            return _.pick(res, 'elementId', 'archiveName', 'archiveVersion', 'id');
          });
          return resourcesTypes;
        }
      }
    ]);
});
