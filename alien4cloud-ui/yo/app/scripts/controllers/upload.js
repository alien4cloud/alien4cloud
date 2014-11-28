/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('UploadCtrl', ['$scope', '$upload',
  function($scope, $upload) {
    //states classes
    var statesToClasses ={
      'error':'danger',
      'success':'success',
      'progress': 'info'
    };

    $scope.uploadInfos = [];
    $scope.upload = [];
    $scope.uploadCtrl = {};

    // take that out as a callback.
    function handleUploadErrors(index, data) {
      if (data.data === null) {
        $scope.uploadInfos[index].otherError = {};
        $scope.uploadInfos[index].otherError.code = data.error.code;
        $scope.uploadInfos[index].otherError.message = data.error.message;
      } else {
        $scope.uploadInfos[index].errors = data.data.errors;
      }
    }

    $scope.doUpload = function(file) {
      var index = $scope.uploadInfos.length;
      $scope.uploadInfos.push({
        'name': file.name,
        'progress': 0,
        'infoType': statesToClasses.progress,
        'isErrorBlocCollapsed': true
      });

      $scope.upload[index] = $upload.upload({
        url: $scope.targetUrl,
        file: file
      }).progress(function(evt) {
        $scope.uploadInfos[index].progress = parseInt(100.0 * evt.loaded / evt.total);
      }).success(function(data) {
        console.log('got response', data);
        // file is uploaded successfully and the server respond without error
        if (data.error === null) {
          $scope.uploadInfos[index].infoType = statesToClasses.success;
          if($scope.uploadSuccessCallback) {
            $scope.uploadSuccessCallback(data);
          }

          if (UTILS.isDefinedAndNotNull(data.data)) {
            $scope.uploadInfos[index].errors = data.data.errors;
          }

        } else {
          $scope.uploadInfos[index].infoType = statesToClasses.error;
          handleUploadErrors(index, data);
        }
      }).error(function(data, status) {
        $scope.uploadInfos[index].infoType = statesToClasses.error;
        $scope.uploadInfos[index].error = {};
        $scope.uploadInfos[index].error.code = status;
        $scope.uploadInfos[index].error.message = 'An Error has occurred on the server!';
      });
    };

    $scope.uploadCtrl.onFileSelect = function($files) {
      for (var i = 0; i < $files.length; i++) {
        var file = $files[i];
        $scope.doUpload(file);
      }
    };

    $scope.uploadCtrl.closeUploadInfos = function(index) {
      $scope.uploadInfos.splice(index, 1);
    };
  }
]);
