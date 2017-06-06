define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/common/services/global_rest_error_handler');
  require('scripts/common/services/alien_resource');
  require('scripts/tosca/directives/node_template_edit');
  require('scripts/services/controllers/service_resource_new');
  require('scripts/users/directives/users_authorization');
  require('scripts/users/directives/groups_authorization');
  require('scripts/users/directives/apps_authorization');
  require('scripts/common/services/resource_security_factory');

  states.state('admin.services', {
    url: '/services',
    templateUrl: 'views/services/service_resources.html',
    controller: 'a4cServiceResourcesCtrl',
    menu: {
      id: 'am.admin.services',
      state: 'admin.services',
      key: 'NAVADMIN.MENU_SERVICES',
      icon: 'fa fa-globe',
      priority: 351
    }
  });

  modules.get('a4c-services', ['ui.router', 'ui.bootstrap','a4c-common']).controller('a4cServiceResourcesCtrl',
    ['$scope', '$uibModal', '$alresource', 'searchServiceFactory', 'resizeServices', 'resourceSecurityFactory', 'globalRestErrorHandler', 'relationshipTypeQuickSearchService',
    function($scope, $uibModal, $alresource, searchServiceFactory, resizeServices, resourceSecurityFactory, globalRestErrorHandler, relationshipTypeQuickSearchService) {
      var serviceResourceService = $alresource('rest/latest/services/:serviceId');
      var orchestratorsService = $alresource('rest/latest/orchestrators/:id');
      var locationsService = $alresource('rest/latest/orchestrators/:orchestratorId/locations/:id');
      var typeWithDependenciesService = $alresource('rest/latest/catalog/types/adv/typewithdependencies/:typeId/:typeVersion');

      $scope.dimensions = { width: 800, height: 500 };
      resizeServices.registerContainer(function (width, height) {
        $scope.dimensions = { width: width, height: height };
        $scope.$digest();
      }, '#catalog-container');

      $scope.relationshipTypeQuickSearchHandler = {
        'doQuickSearch': relationshipTypeQuickSearchService.doQuickSearch,
        'waitBeforeRequest': 500,
        'minLength': 3
      };

      $scope.locations = [];
      // Fetch all orchestrators and locations for display
      orchestratorsService.get({}, null, function(orchestratorResult){
        if(_.undefined(orchestratorResult.data) || _.undefined(orchestratorResult.data.data)) { return; }
        _.each(orchestratorResult.data.data, function(orchestrator){
          locationsService.get({
            orchestratorId: orchestrator.id
          }, null, function(locationResult){
            if(_.undefined(locationResult.data)) { return; }
            _.each(locationResult.data, function(location) {
              location.location.orchestratorName = orchestrator.name;
              $scope.locations.push(location.location);
            });
          });
        });
      });

      $scope.query = '';
      // onSearchCompleted is used as a callaback for the searchServiceFactory and triggered when the search operation is completed.
      $scope.onSearchCompleted = function(searchResult) {
        $scope.serviceResources = searchResult.data.data;
      };
      // we have to insert the search service in the scope so it is available for the pagination directive.
      $scope.searchService = searchServiceFactory('rest/latest/services/adv/search', false, $scope, 50);
      $scope.search = function() {$scope.searchService.search();};
      $scope.search(); // initialize

      $scope.addService = function(dragData) {
        var resourceType = angular.fromJson(dragData.source);
        if (!resourceType) {
          return;
        }

        $scope.createServiceRequest = {
          nodeType: resourceType.elementId,
          nodeTypeVersion: resourceType.archiveVersion
        };
        // Open modal to ask name and version from the user
        var modalInstance = $uibModal.open({
          templateUrl: 'views/services/service_resource_new.html',
          controller: 'a4cNewServiceResourceCtrl',
          scope: $scope
        });

        modalInstance.result.then(function(createServiceRequest) {
          serviceResourceService.create([], angular.toJson(createServiceRequest), function() {
            $scope.search(); // refresh the view
          });
        });
      };

      function setSelectedTypesToScope() {
        $scope.selectedNodeType = $scope.selectedService.uiNodeType;
        $scope.selectedCapabilityTypes = $scope.selectedService.uiCapabilityTypes;
        $scope.selectedDependencies = $scope.selectedService.uiDependencies;
        $scope.selectedDataTypes = $scope.selectedService.uiDataTypes;
      }

      var clearSelection = function(){
        delete $scope.selectedService;
        delete $scope.selectedNodeType;
        delete $scope.stateDisabled;
      };

      $scope.clearSelection = clearSelection;

      $scope.selectService = function(service) {
        clearSelection();
        $scope.selectedService = service;

        // We have to fetch the node type, the dependencies, and the related capabilities
        if(_.defined(service)) {
          $scope.stateDisabled = service.nodeInstance.attributeValues.state === 'initial';
          if(_.undefined(service.uiNodeType)) {
            typeWithDependenciesService.get({
              typeId: service.nodeInstance.nodeTemplate.type,
              typeVersion: service.nodeInstance.typeVersion
            }, null, function(result) {
              $scope.selectedService.uiNodeType = result.data.toscaType;
              $scope.selectedService.uiCapabilityTypes = result.data.capabilityTypes;
              $scope.selectedService.uiDependencies = result.data.dependencies;
              $scope.selectedService.uiDataTypes = result.data.dataTypes;
              setSelectedTypesToScope();
            });
          } else {
            setSelectedTypesToScope();
          }
        }
      };

      $scope.toggleState = function() {
        var newState = 'initial';
        if($scope.stateDisabled) {
          newState = 'started';
        }
        var updateRequest = { nodeInstance: { attributeValues:{ state: newState} } };
        return serviceResourceService.patch({
          serviceId: $scope.selectedService.id
        }, angular.toJson(updateRequest), function(result) {
          globalRestErrorHandler.handle(result);
          if(_.undefined(result.error)){
            $scope.selectedService.nodeInstance.attributeValues.state = newState;
            $scope.stateDisabled = !$scope.stateDisabled;
          }
        });
      };

      $scope.isLocAuthorized = function(targetLocation) {
        if(_.undefinedPath($scope, 'selectedService.locationIds')) {
          return false;
        }
        return $scope.selectedService.locationIds.indexOf(targetLocation.id) !== -1;
      };

      $scope.toggleLoc = function(targetLocation) {
        if(_.undefined($scope.selectedService)) {
          return; // should not be able to trigger this.
        }
        if(_.undefined($scope.selectedService.locationIds)) {
          $scope.selectedService.locationIds = [];
        }
        var add = $scope.selectedService.locationIds.indexOf(targetLocation.id) === -1;
        var updatedLocations;
        if(add) {
          updatedLocations = _.concat($scope.selectedService.locationIds, [targetLocation.id]);
        } else {
          updatedLocations = _.without($scope.selectedService.locationIds, targetLocation.id);
        }

        return serviceResourceService.patch({
          serviceId: $scope.selectedService.id
        }, angular.toJson({locationIds: updatedLocations}), function(){
          $scope.selectedService.locationIds = updatedLocations;
        });
      };

      // not editable:
      // - enabled services
      // - managed services
      $scope.isPropertyEditable = function() {
        return $scope.stateDisabled && _.undefined($scope.selectedService.environmentId) ;
      };

      $scope.updateProperty= function(propertyName, propertyValue) {
        var updateRequest = { nodeInstance: { properties:{} } };
        updateRequest.nodeInstance.properties[propertyName] = { value: propertyValue };
        return serviceResourceService.patch({
          serviceId: $scope.selectedService.id
        }, angular.toJson(updateRequest)).$promise;
      };

      $scope.updateHalfRelationshipType = function(type, name, relationshipTypeId) {
        var updateRequest;
        switch(type){
          case 'capability':
            updateRequest = {
              capabilitiesRelationshipTypes: {}
            };
            updateRequest.capabilitiesRelationshipTypes[name] = relationshipTypeId;
            break;
          case 'requirement':
            updateRequest = {
              requirementsRelationshipTypes:{}
            };
            updateRequest.requirementsRelationshipTypes[name] = relationshipTypeId;
            break;
        }
        return serviceResourceService.patch({
          serviceId: $scope.selectedService.id
        }, angular.toJson(updateRequest)).$promise;
      };

      $scope.updateCapabilityProperty = function(capabilityName, propertyName, propertyValue) {
        var updateRequest = { nodeInstance: { capabilities:{} } };
        updateRequest.nodeInstance.capabilities[capabilityName] = {properties:{}};
        updateRequest.nodeInstance.capabilities[capabilityName].properties[propertyName] = propertyValue;
        return serviceResourceService.patch({
          serviceId: $scope.selectedService.id
        }, angular.toJson(updateRequest)).$promise;
      };

      $scope.updateAttribute = function(attributeName, attributeValue) {
        var updateRequest = { nodeInstance: { attributeValues:{} } };
        updateRequest.nodeInstance.attributeValues[attributeName] = attributeValue;
        return serviceResourceService.patch({
          serviceId: $scope.selectedService.id
        }, angular.toJson(updateRequest)).$promise;
      };

      $scope.update = function(updateRequest) {
        // This may be triggered by editable form so it must return the promise.
        return serviceResourceService.patch({
          serviceId: $scope.selectedService.id
        }, angular.toJson(updateRequest)).$promise;
      };

      $scope.delete = function(serviceId) {
        serviceResourceService.delete({
          serviceId: serviceId
        }, null, function(result) {
          globalRestErrorHandler.handle(result);
          if(_.undefined(result.error)){
            $scope.search();
            if (_.get($scope, 'selectedService.id') === serviceId) {
              clearSelection();
            }
          }
        });
      };

      $scope.servicesSecurityService = resourceSecurityFactory('rest/latest/services/:serviceId', {
        serviceId: function () {
          return $scope.selectedService.id;
        }
      });
    }
  ]); // controller
}); // define
