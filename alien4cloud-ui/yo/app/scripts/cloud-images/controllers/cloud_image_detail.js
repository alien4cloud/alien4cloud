'use strict';

angular.module('alienUiApp').controller(
  'CloudImageDetailController', ['$scope', '$state', 'cloudImageServices', 'cloudImage', '$upload',
    function($scope, $state, cloudImageServices, cloudImage, $upload) {
      $scope.cloudImage = cloudImage;
      $scope.cloudImageFormDescriptor = cloudImageServices.getFormDescriptor();
      $scope.cloudImageFormDescriptor._propertyType.name._isTitle = true;
      if (UTILS.isUndefinedOrNull($scope.cloudImage.requirement)) {
        $scope.requirement = {};
      } else {
        $scope.requirement = $scope.cloudImage.requirement;
      }

      $scope.requirementDescriptor = cloudImageServices.requirementDescriptor;

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

      $scope.updateName = function(name) {
        cloudImageServices.update({
          id: $scope.cloudImage.id
        }, angular.toJson({
          'name': name
        }), null);
      };
    }
  ]);
