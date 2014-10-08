'use strict';

angular.module('alienUiApp').controller(
  'CloudImageListController', [
    '$scope',
    '$state',
    'searchServiceFactory',
    '$modal',
    'cloudImageServices',
    function($scope, $state, searchServiceFactory, $modal, cloudImageServices) {

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
          $scope.goToCloudImage(cloudImageId);
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
  ]);
