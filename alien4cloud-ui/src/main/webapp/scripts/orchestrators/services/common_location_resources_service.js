define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/tosca/services/tosca_service');
  require('scripts/common/services/resource_security_factory');
  require('scripts/orchestrators/services/location_resources_processor');

  modules.get('a4c-orchestrators').factory('commonLocationResourcesService', ['toscaService', 'resourceSecurityFactory', 'locationResourcesPortabilityService', 'locationResourcesProcessor',
    function (toscaService, resourceSecurityFactory, locationResourcesPortabilityService, locationResourcesProcessor)  {

      /**
      * subjectResource ==> resources, policies
      *
      *crudCallbacks: Object holding calbakcs for CRUD request
         add: function(restResponse, resourceSummary {resourceName, resourceType, archiveName, archiveVersion, sourceId} )
      */
      return function(scope, subjectResource, resourceService, resourcePropertyService, crudCallbacks) {

        function computeTypes() {
          // pick all resource types from the location - this will include orchestrator & custom types
          var provided = scope.providedTypes || _.map(scope.resourcesTypes, 'elementId');
          return _.map(scope.resourcesTypes, function (res) {
            return _.assign(
              _.pick(res, 'elementId', 'archiveName', 'archiveVersion', 'id'),
              {'provided': _.contains(provided, res.elementId)}
            );
          });
        }

        var init = function(){
          delete scope.selectedResourceTemplate;
          scope.resourcesTypes = _.values(scope.resourcesTypesMap);
          scope.favorites = computeTypes();
        };

        scope.$watch('resourcesTemplates', function(){
          init();
        });

        scope.selectTemplate = function(template) {
          scope.selectedResourceTemplate = template;
        };

        scope.getIcon = function(resourceType) {
          var templateType = scope.resourcesTypesMap[resourceType];
          return toscaService.getElementIcon(templateType);
        };

        scope.updateLocationResource = function(propertyName, propertyValue) {
          var request = {};
          request[propertyName] = propertyValue;
          return resourceService.update({
            orchestratorId: scope.context.orchestrator.id,
            locationId: scope.context.location.id,
            id: scope.selectedResourceTemplate.id
          }, angular.toJson(request)).$promise;
        };


        scope.addResourceTemplate = function(dragData) {
          var source = angular.fromJson(dragData.source);

          if (!source) {
            return;
          }

          var newResource = {
            resourceType: source.elementId,
            resourceName: 'New resource',
            archiveName: source.archiveName || '',
            archiveVersion: source.archiveVersion || '',
          };

          resourceService.save({
            orchestratorId: scope.context.orchestrator.id,
            locationId: scope.context.location.id
          }, angular.toJson(newResource), function(response) {
            //always do this
            locationResourcesProcessor.processTemplate(response.data.resourceTemplate);
            scope.context.location.dependencies = response.data.newDependencies;

            // then call the provided callback if needed
            if(_.isFunction(_.get(crudCallbacks, 'add'))){
              newResource.sourceId=source.id;
              crudCallbacks.add(response, newResource);
            }else {
              scope.resourcesTemplates.push(response.data.resourceTemplate);
              scope.selectTemplate(response.data.resourceTemplate);
            }
          });
        };


        // delete is called from the directive but must be managed here as we must delete the selectedResourceTemplate on success
        scope.deleteResourceTemplate = function(resourceTemplate) {
          resourceService.delete({
            orchestratorId: scope.context.orchestrator.id,
            locationId: scope.context.location.id,
            id: scope.selectedResourceTemplate.id
          }, undefined, function() {
            //always do this
            _.remove(scope.resourcesTemplates, {
              id: resourceTemplate.id
            });
            delete scope.selectedResourceTemplate;
            // then call the provided callback if needed
            if(_.isFunction(_.get(crudCallbacks, 'delete'))){
              crudCallbacks.delete(resourceTemplate);
            }
          });
        };


        scope.updateResourceProperty = function(propertyName, propertyValue) {
          return resourcePropertyService.save({
            orchestratorId: scope.context.orchestrator.id,
            locationId: scope.context.location.id,
            id: scope.selectedResourceTemplate.id
          }, angular.toJson({
            propertyName: propertyName,
            propertyValue: propertyValue
          })).$promise;
        };

        scope.updatePortabilityProperty = function(propertyName, propertyValue) {
          return locationResourcesPortabilityService.save({
            orchestratorId: scope.context.orchestrator.id,
            locationId: scope.context.location.id,
            id: scope.selectedResourceTemplate.id
          }, angular.toJson({
            propertyName: propertyName,
            propertyValue: propertyValue
          })).$promise;
        };

        scope.isPropertyEditable = function() {
          return true;
        };

        /************************************
        *  For authorizations directives
        /************************************/

        // NOTE: locationId and resourceId are functions, so that it will be evaluated everytime a REST call will be made
        // this is because the selected location / resource can change within the page
        var locationResourcesSecurityService = resourceSecurityFactory('rest/latest/orchestrators/:orchestratorId/locations/:locationId/'+subjectResource+'/:resourceId', {
          orchestratorId: scope.context.orchestrator.id,
          locationId: function(){ return scope.context.location.id;},
          resourceId: function(){ return _.get(scope.selectedResourceTemplate,'id');}
        });
        scope.locationResourcesSecurityService = locationResourcesSecurityService;

        //NOTE: locationId is not defined a function here, since buildSecuritySearchConfig itself will be called from the directive controller
        // therefore, even if the selected location changes, it will always be updated  on the directive side.
        /*subject can be users, groups, applications*/
        scope.buildSecuritySearchConfig = function(subject){
          return {
            url: 'rest/latest/orchestrators/:orchestratorId/locations/:locationId/security/' + subject + '/search',
            useParams: true,
            params: {
              orchestratorId: scope.context.orchestrator.id,
              locationId: scope.context.location.id
            }
          };
        };

        scope.context.selectedResourceTemplates = {};

        scope.toggleTemplate = function(template, $event) {
          //prevent selection of the template
          if(_.defined($event)){
            $event.stopPropagation();
          }
          delete scope.selectedResourceTemplate;
          if (scope.isSelected(template)) {
            delete scope.context.selectedResourceTemplates[template.id];
          } else {
            scope.context.selectedResourceTemplates[template.id] = template;
          }
        };

        scope.isSelected = function(template) {
          return _.defined(scope.context.selectedResourceTemplates[template.id]);
        };

        scope.toggleAllTemplates = function() {
          if (Object.keys(scope.context.selectedResourceTemplates).length === 0) {
            for (var i in scope.resourcesTemplates) {
              scope.toggleTemplate(scope.resourcesTemplates[i]);
            }
          } else {
            for (var j in scope.context.selectedResourceTemplates) {
              scope.toggleTemplate(scope.context.selectedResourceTemplates[j]);
            }
          }
        };

        scope.allTemplatesAreSelected = function() {
          return Object.keys(scope.context.selectedResourceTemplates).length === Object.keys(scope.resourcesTemplates).length && Object.keys(scope.resourcesTemplates).length > 0;
        };

        scope.disableSecurity = function() {
          return Object.keys(scope.context.selectedResourceTemplates).length !== 0;
        };

      };

    }]);
});
