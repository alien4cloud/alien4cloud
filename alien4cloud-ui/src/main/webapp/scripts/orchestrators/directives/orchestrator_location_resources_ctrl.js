/** global Promise */
define(function(require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/directives/orchestrator_location_resource_template');
  require('scripts/orchestrators/services/location_resources_processor');
  require('scripts/orchestrators/services/common_location_resources_service');

  require('scripts/common/services/resize_services');
  require('scripts/components/services/component_services');


  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('OrchestratorLocationResourcesTemplateCtrl',
    ['$scope', 'locationResourcesService', 'locationResourcesPropertyService', 'locationResourcesCapabilityPropertyService',
      'locationResourcesProcessor', 'resizeServices', 'componentService',
      'commonLocationResourcesService',
      function($scope, locationResourcesService, locationResourcesPropertyService,
               locationResourcesCapabilityPropertyService, locationResourcesProcessor,
               resizeServices, componentService, commonLocationResourcesService) {

        $scope.catalogType = 'NODE_TYPE';
        $scope.resourceTemplateEditDisplayUrl = 'views/orchestrators/includes/location_resource_template_edit.html';

        $scope.dimensions = { width: 800, height: 500 };
        resizeServices.registerContainer(function (width, height) {
          $scope.dimensions = { width: width, height: height };
          $scope.$digest();
        }, '#resource-catalog');

        var processAfterAddingResource = function(response, newResource){
          locationResourcesProcessor.processLocationResourceTemplate(response.data.resourceTemplate);
          // if ResourceType is not in the fav list then get its type and add it to resource types map
          console.log($scope.favorites);
          if ($scope.showCatalog && newResource && _.findIndex($scope.favorites, 'id', newResource.sourceId) === -1) {
            var typeId = newResource.resourceType;
            var componentId = newResource.sourceId;

            componentService.get({componentId: componentId}).$promise.then(function (res) {
              var resourceType = res.data;
              var promises = [];
              _.forEach(resourceType.capabilities, function(capability) {
                if (!_.has($scope.context.locationResources.capabilityTypes, capability.type)) {
                // Query capability type if needed using getInArchives - passing the location dependencies
                  var p = componentService.getInArchives(capability.type, 'CAPABILITY_TYPE', response.data.newDependencies)
                    .then(function (res) {
                      var capabilityType = res.data.data;
                      capabilityType.propertiesMap = _.keyBy(capabilityType.properties, 'key');
                      $scope.context.locationResources.capabilityTypes[capability.type] = capabilityType;
                    });
                  promises.push(p);
                }
              });

              // keep track of promises to wait before selecting the template. Use $apply to make sure watchers are triggered.
              Promise.all(promises).then(function () {
                $scope.$apply(function() {
                  // select the template.
                  $scope.selectTemplate(response.data.resourceTemplate);
                });
              });

              // Compute properties map and update scope right after getting the resource type.
              resourceType.propertiesMap = _.keyBy(resourceType.properties, 'key');
              $scope.resourcesTypesMap[typeId] = resourceType;
              $scope.resourcesTypes.push(resourceType);
              $scope.resourcesTemplates.push(response.data.resourceTemplate);

              // property named elementId is expected instead of resourceType
              newResource.elementId = newResource.resourceType;
              delete newResource.resourceType;
              // property named id is expected instead of sourceId
              newResource.id = newResource.sourceId;
              delete newResource.sourceId;
              newResource.provided = false;
              $scope.favorites.push(newResource);
            });
          } else {
            // If the type is in the fav list then we already have its node and capability types
            // its either an orchestrator resource or a custom-resource already used in the location.
            $scope.resourcesTemplates.push(response.data.resourceTemplate);
            $scope.selectTemplate(response.data.resourceTemplate);
          }
        };

        var processAfterDeletingingResource = function(resource){
          // Clean the favorites list
          var deletedType = resource.template.type;
          // If the type of the template is provided by the orchestrator, never delete it from the fav list
          var favIndex = _.findIndex($scope.favorites, { 'elementId': deletedType });
          if (favIndex === -1 || $scope.favorites[favIndex].provided) {
            return;
          }
          // The template was a custom resource - if its still used do not delete it from the fav list
          if (_.find($scope.resourcesTemplates, function (tplt) { return tplt.template.type === deletedType; })) {
            return;
          }
          $scope.favorites.splice(favIndex, 1);
        };

        commonLocationResourcesService($scope, 'resources', locationResourcesService, locationResourcesPropertyService, {
          add: processAfterAddingResource,
          delete: processAfterDeletingingResource
        });

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

      }
    ]);
});
