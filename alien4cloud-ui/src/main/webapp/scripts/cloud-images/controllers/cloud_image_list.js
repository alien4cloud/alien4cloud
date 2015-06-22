// list of cloud images that can be defined for multiple clouds actually.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/cloud-images/controllers/cloud_image_detail');
  require('scripts/cloud-images/controllers/new_cloud_image');
  require('scripts/cloud-images/services/cloud_image_services');

  require('scripts/common/directives/pagination');

  states.state('admin.cloud-images', {
    url: '/cloud-images',
    template: '<ui-view/>',
    menu: {
      id: 'am.admin.cloud-images.list',
      state: 'admin.cloud-images',
      key: 'NAVADMIN.MENU_CLOUD_IMAGES',
      icon: 'fa fa-image',
      priority: 400
    }
  });
  states.state('admin.cloud-images.list', {
    url: '/list',
    templateUrl: 'views/cloud-images/cloud_image_list.html',
    controller: 'CloudImageListController'
  })
  states.forward('admin.cloud-images', 'admin.cloud-images.list');

  modules.get('a4c-clouds', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('CloudImageListController',
    ['$scope', '$state', '$modal', 'searchServiceFactory',  'cloudImageServices',
    function($scope, $state, $modal, searchServiceFactory,  cloudImageServices) {
      $scope.query = '';
      $scope.onSearchCompleted = function(searchResult) {
        $scope.data = searchResult.data;
      };
      $scope.searchService = searchServiceFactory('rest/cloud-images/search', false, $scope, 20);

      $scope.search = function() {
        $scope.searchService.search();
      };

      // first load
      $scope.search();

      /** handle Modal form for cloud image creation */
      $scope.openNewModal = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/cloud-images/new_cloud_image.html',
          controller: 'NewCloudImageController',
          windowClass: 'newImageModal'
        });

        modalInstance.result.then(function(cloudImageId) {
          $state.go('admin.cloud-images.detail', {id: cloudImageId, mode: 'edit'});
        });
      };

      $scope.goToCloudImage = function(id) {
        $state.go('admin.cloud-images.detail', {id: id});
      };

      $scope.delete = function(id) {
        cloudImageServices.remove({
          id: id
        }, undefined, function() {
          $scope.search();
        });
      };
    }
  ]); // controller
}); // define
