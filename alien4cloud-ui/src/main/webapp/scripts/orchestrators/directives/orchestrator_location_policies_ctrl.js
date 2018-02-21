/** global Promise */
define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/orchestrators/services/location_resources_processor');
  require('scripts/orchestrators/directives/orchestrator_location_policy_template');
  require('scripts/orchestrators/services/common_location_resources_service');
  require('scripts/components/services/component_services');

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('OrchestratorLocationPoliciesTemplateCtrl',
    ['$scope', 'commonLocationResourcesService', 'locationPoliciesService', 'locationPoliciesPropertyService', 'locationResourcesProcessor', 'componentService',
      function($scope, commonLocationResourcesService, locationPoliciesService, locationPoliciesPropertyService, locationResourcesProcessor, componentService) {
        $scope.catalogType = 'POLICY_TYPE';

        $scope.resourceTemplateEditDisplayUrl = 'views/orchestrators/includes/location_policy_template_edit.html';

        var processAfterAddingResource = function(response, newResource){
          locationResourcesProcessor.processTemplate(response.data.resourceTemplate);
          // if ResourceType is not in the fav list then get its type and add it to resource types map
          if ($scope.showCatalog && newResource && _.findIndex($scope.favorites, 'id', newResource.sourceId) === -1) {
            var typeId = newResource.resourceType;
            var componentId = newResource.sourceId;

            componentService.get({componentId: componentId, toscaType:'POLICY_TYPE'}).$promise.then(function (res) {
              var resourceType = res.data;

              // Compute properties map and update scope right after getting the resource type.
              resourceType.propertiesMap = _.indexBy(resourceType.properties, 'key');
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

              $scope.selectTemplate(response.data.resourceTemplate);
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

        commonLocationResourcesService($scope, 'policies', locationPoliciesService, locationPoliciesPropertyService, {
          add: processAfterAddingResource,
          delete: processAfterDeletingingResource
        });

      }
    ]);
});
