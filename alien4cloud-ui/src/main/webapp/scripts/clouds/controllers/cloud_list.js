// list of cloud images that can be defined for multiple clouds actually.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/cloud-images/controllers/cloud_image_list');
  require('scripts/clouds/services/cloud_services');
  require('scripts/clouds/controllers/new_cloud');
  require('scripts/clouds/controllers/cloud_detail');

  states.state('admin.clouds', {
    url: '/clouds',
    template: '<ui-view/>',
    menu: {
      id: 'am.admin.clouds.list',
      state: 'admin.clouds.list',
      key: 'NAVADMIN.MENU_CLOUDS',
      icon: 'fa fa-cloud',
      priority: 300
    }
  });
  states.state('admin.clouds.list', {
    url: '/list',
    templateUrl: 'views/clouds/cloud_list.html',
    controller: 'CloudListController'
  });
  states.forward('admin.clouds', 'admin.clouds.list');

  modules.get('a4c-clouds', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('CloudListController',
    ['$scope', '$state', 'searchServiceFactory', '$modal', 'cloudServices',
    function($scope, $state, searchServiceFactory, $modal, cloudServices) {
      $scope.query = '';
      $scope.onSearchCompleted = function(searchResult) {
        $scope.data = searchResult.data;
      };
      $scope.searchService = searchServiceFactory('rest/clouds/search', true, $scope, 20);

      $scope.search = function() {
        $scope.searchService.search();
      };

      // first load
      $scope.search();

      /** handle Modal form for user creation */
      $scope.openNewModal = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/clouds/new_cloud.html',
          controller: 'NewCloudController'
        });

        modalInstance.result.then(function(newCloud) {
          var cloud = {
            name: newCloud.name,
            paasPluginId: newCloud.paaSProvider.pluginId,
            paasPluginBean: newCloud.paaSProvider.componentDescriptor.beanName,
            paasProviderName: newCloud.paaSProvider.componentDescriptor.name
          };

          cloudServices.create([], angular.toJson(cloud), function() {
            $scope.searchService.search();
          });
        });
      };

      $scope.openCloud = function(id) {
        $state.go('admin.clouds.detail', {id : id});
      };

      $scope.cloneCloud = function(id) {
        cloudServices.cloneCloud({
          id: id
        }, {}, function(response) {
          if (_.defined(response.data)) {
            $scope.openCloud(response.data);
          }
        });
      };
    }
  ]);
});
