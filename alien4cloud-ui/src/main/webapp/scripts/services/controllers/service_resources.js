define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/common/services/alien_resource');
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
      const serviceResourceService = $alresource('rest/latest/services/:id');

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
      };
    }
  ]); // controller
}); // define
