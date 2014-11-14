'use strict';

angular.module('alienUiApp').controller(
  'CloudImageDetailController', ['$scope', '$state', 'cloudImageServices', 'cloudImage', '$upload',
    function($scope, $state, cloudImageServices, cloudImage, $upload) {
      $scope.cloudImage = cloudImage;

      $scope.cloudImageFormDescriptor = cloudImageServices.getFormDescriptor();
      $scope.cloudImageFormDescriptor._propertyType.name._isTitle = true;
      $scope.cloudImageFormDescriptor._propertyType.osType._isReadOnly = true;
      $scope.cloudImageFormDescriptor._propertyType.osDistribution._isReadOnly = true;
      $scope.cloudImageFormDescriptor._propertyType.osVersion._isReadOnly = true;
      $scope.cloudImageFormDescriptor._propertyType.osArch._isReadOnly = true;
      $scope.cloudImageFormDescriptor._propertyType.numCPUs._isReadOnly = true;
      $scope.cloudImageFormDescriptor._propertyType.diskSize._isReadOnly = true;
      $scope.cloudImageFormDescriptor._propertyType.memSize._isReadOnly = true;

      // Upload handler
      $scope.doUpload = function(file) {
        $upload.upload({
          url: 'rest/cloud-images/' + $scope.cloudImage.id + '/icon',
          file: file
        }).success(function(result) {
          $scope.cloudImage.iconId = result.data;
        });
      };

      $scope.onIconSelected = function($files) {
        var file = $files[0];
        $scope.doUpload(file);
      };

      $scope.delete = function() {
        cloudImageServices.remove({
          id: $scope.cloudImage.id
        }, undefined, function() {
          $state.go('admin.cloud-images.list');
        });
      };

      $scope.save = function(object) {
        cloudImageServices.update({
          id: $scope.cloudImage.id
        }, angular.toJson(object));
      };
    }
  ]);
