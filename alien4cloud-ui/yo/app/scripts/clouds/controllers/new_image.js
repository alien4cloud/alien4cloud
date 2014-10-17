'use strict';

angular.module('alienUiApp').controller(
  'AddCloudImageController', [
    '$scope',
    '$modalInstance',
    'searchServiceFactory',
    function($scope, $modalInstance, searchServiceFactory) {

      $scope.modalData = {};

      $scope.images = [];

      $scope.query = '';

      $scope.queryChanged = function(query) {
        $scope.query = query;
      };

      $scope.onSearchCompleted = function(searchResult) {
        $scope.data = searchResult.data;
      };

      $scope.initSearchService = function() {
        var imageIdsToExclude = [];
        for (var i = 0; i < $scope.images.length; i++) {
          imageIdsToExclude.push($scope.images[i].id);
        }
        imageIdsToExclude = UTILS.concat(imageIdsToExclude, $scope.cloud.images);
        $scope.searchService = searchServiceFactory('rest/cloud-images/search', false, $scope, 3, undefined, undefined, undefined, {
          exclude: imageIdsToExclude
        });
      };

      $scope.search = function() {
        $scope.searchService.search();
      };

      // first load
      $scope.initSearchService();
      $scope.search();

      $scope.selectImage = function(cloudImage) {
        $scope.images.push(cloudImage);
        $scope.initSearchService();
        $scope.search();
      };

      $scope.unSelectImage = function(cloudImage) {
        var indexOfImage = UTILS.findByFieldValue($scope.images, 'id', cloudImage.id);
        $scope.images.splice(indexOfImage, 1);
        $scope.initSearchService();
        $scope.search();
      };

      $scope.ok = function() {
        $modalInstance.close($scope.images);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('close');
      };
    }
  ]
);
