define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/common/services/alien_resource');
  require('scripts/tosca/directives/node_template_edit');
  require('scripts/services/controllers/service_resource_new');

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
    ['$scope', '$uibModal', '$alresource', 'searchServiceFactory', 'resizeServices',
    function($scope, $uibModal, $alresource, searchServiceFactory, resizeServices) {
      const serviceResourceService = $alresource('rest/latest/services/:serviceId');
      const typeWithDependenciesService = $alresource('rest/latest/catalog/types/adv/typewithdependencies/:typeId/:typeVersion');

      $scope.dimensions = { width: 800, height: 500 };
      resizeServices.registerContainer(function (width, height) {
        $scope.dimensions = { width: width, height: height };
        $scope.$digest();
      }, '#catalog-container');

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
            $scope.searchService.search(); // refresh the view
          });
        });
      };

      $scope.selectService = function(service) {
        $scope.selectedService = service;
        delete $scope.nodeType;

        // We have to fetch the node type, the dependencies, and the related capabilities
        if(_.defined(service)) {
          typeWithDependenciesService.get({
            typeId: service.nodeInstance.nodeTemplate.type,
            typeVersion: service.nodeInstance.typeVersion
          }, null, function(result){
            $scope.selectedNodeType = result.data.toscaType;
            $scope.selectedCapabilityTypes = result.data.capabilityTypes;
            $scope.selectedDependencies = result.data.dependencies;
          });
        }
      };

      $scope.isPropertyEditable = function() { return true; };

      $scope.updateProperty= function(propertyName, propertyValue) {
        console.log('property update', propertyName, propertyValue);

        var updateRequest = {nodeInstance: {nodeTemplate: {properties:{}}}};
        updateRequest.nodeInstance.nodeTemplate.properties[propertyName] = propertyValue;
        return serviceResourceService.update({
          serviceId: $scope.selectedService.id
        }, angular.toJson(updateRequest)).$promise;
      };

      $scope.updateCapabilityProperty = function(capabilityName, propertyName, propertyValue) {
        console.log('update capability property', capabilityName, propertyName, propertyValue);
        var updateRequest = {nodeInstance: {nodeTemplate: {capabilities:{}}}};
        updateRequest.nodeInstance.nodeTemplate.capabilities[capabilityName] = {properties:{}};
        updateRequest.nodeInstance.nodeTemplate.capabilities[capabilityName].properties[propertyName] = propertyValue;
        return serviceResourceService.update({
          serviceId: $scope.selectedService.id
        }, angular.toJson(updateRequest)).$promise;
      };

      $scope.update = function(updateRequest) {
        // This may be triggered by editable form so it must return the promise.
        return serviceResourceService.update({
          serviceId: $scope.selectedService.id
        }, angular.toJson(updateRequest)).$promise;
      };

      $scope.delete = function(serviceId) {
        serviceResourceService.delete({
          serviceId: serviceId
        }, null, function(){
          if(_.defined($scope.selectedService) && $scope.selectedService.id === serviceId) {
            $scope.selectedService = undefined;
          }
        });
      };
    }
  ]); // controller
}); // define
