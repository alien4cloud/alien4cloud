define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');
  require('angular-file-upload');

  modules.get('a4c-common').directive('uploadDirective', function() {
    return {
      templateUrl : 'views/common/upload_template.html',
      restrict : 'E',
      scope : {
        'targetUrl' : '&',
        'requestData': '&',
        'dragAndDropMessage' : '=',
        'buttonMessage' : '=',
        'beforeUploadCallback': '&', // should take two param named 'scope' and 'files'
        'uploadSuccessCallback': '&'
      }
    };
  });

  modules.get('a4c-common', ['angularFileUpload']).controller('UploadCtrl', [ '$scope', '$upload', '$q', function($scope, $upload, $q) {
    // states classes
    var statesToClasses = {
      'error': 'danger',
      'success': 'success',
      'progress': 'info'
    };

    $scope.uploadInfos = [];
    $scope.upload = [];
    $scope.uploadCtrl = {};

    // take that out as a callback.
    function handleUploadErrors(index, data) {
      if (_.undefined(data.data)) {
        $scope.uploadInfos[index].otherError = {};
        $scope.uploadInfos[index].otherError.code = data.error.code;
        $scope.uploadInfos[index].otherError.message = data.error.message;
      } else if (_.defined(data.data.errors) && _.size(data.data.errors) > 0) {
        $scope.uploadInfos[index].errors = data.data.errors;
      }
    }


//build the upload directive data
    function buildUploadData(file){
      var data = {file: file};

      //these can be simple values or functions
      var url = angular.isFunction($scope.targetUrl()) ? $scope.targetUrl()() : $scope.targetUrl();
      var requestData = angular.isFunction($scope.requestData()) ? $scope.requestData()() : $scope.requestData();

      if(_.defined(url)){
        data.url=url;
      }
      if(_.defined(requestData)){
        data.data = requestData;
      }
      return data;
    }

    $scope.doUpload = function(file) {

      var uploadData =  buildUploadData(file);
      var index = $scope.uploadInfos.length;
      $scope.uploadInfos.push({
        'name': file.name,
        'progress': 0,
        'infoType': statesToClasses.progress,
        'isErrorBlocCollapsed': true
      });


      $scope.upload[index] = $upload.upload(uploadData).progress(function(evt) {
        $scope.uploadInfos[index].progress = parseInt(100.0 * evt.loaded / evt.total);
      }).success(function(data) {
        // file is uploaded successfully and the server respond without error
        if (data.error === null) {
          $scope.uploadInfos[index].infoType = statesToClasses.success;
          if ($scope.uploadSuccessCallback) {
            $scope.uploadSuccessCallback(data);
          }

          // there might be warnings. display them
          if (_.defined(data.data) && _.defined(data.data.errors) && _.size(data.data.errors) >0) {
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

    function uploadFiles($files){
      for (var i = 0; i < $files.length; i++) {
        var file = $files[i];
        $scope.doUpload(file);
      }
    }

    $scope.uploadCtrl.onFileSelect = function($files) {
      // if there is a callback for before uploding, then call it first
      if($scope.beforeUploadCallback()){
        $q.when($scope.beforeUploadCallback()($scope, $files), function(){
          uploadFiles($files);
        });
      }else{
        uploadFiles($files);
      }
    };

    $scope.uploadCtrl.closeUploadInfos = function(index) {
      $scope.uploadInfos.splice(index, 1);
    };
  }]);
});
