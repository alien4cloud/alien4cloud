define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');
  require('angular-file-upload');
  require('scripts/common/directives/parsing_errors');

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
        'uploadSuccessCallback': '&',
        'getWorkspaceSpecifics': '&?' // should take one param named 'scope'
      }
    };
  });

  modules.get('a4c-common').factory('uploadServiceFactory',['Upload',function($upload) {
    var FileUploadManager = function(scope) {
      this.scope = scope;
      this.scope.uploadInfos = [];
      this.scope.upload = [];
      this.scope.uploadCtrl = {};
    };

    function setInfoType(uploadInfos, errors, statesToClasses) {
      if (_.defined(_.find(_.flatten(_.values(errors)), {'errorLevel': 'WARNING'}))) {
        //change the displayed class into warning if there is a warning
        uploadInfos.infoType = statesToClasses.warn;
      } else {
        uploadInfos.infoType = statesToClasses.success;
      }
    }

    FileUploadManager.prototype = {
      constructor: FileUploadManager,
      statesToClasses: {
        'error': 'danger',
        'success': 'success',
        'progress': 'info',
        'warn': 'warning'
      },
      handleUploadErrors: function(index, data) {
        if (_.undefined(data.data)) {
          this.scope.uploadInfos[index].otherError = {
            code: data.error.code,
            message: data.error.message
          };
        } else if (_.isNotEmpty(data.data.errors)) {
          this.scope.uploadInfos[index].errors = data.data.errors;
        }
      },
      doUpload: function(file, uploadData) {
        var self = this;
        var index = this.scope.uploadInfos.length;
        this.scope.uploadInfos.push({
          'name': file.name,
          'progress': 0,
          'infoType': self.statesToClasses.progress,
          'isErrorBlocCollapsed': true
        });

        this.scope.upload[index] = $upload.upload(uploadData).progress(function(evt) {
          self.scope.uploadInfos[index].progress = parseInt(100.0 * evt.loaded / evt.total);
        }).success(function(data) {
          // file is uploaded successfully and the server respond without error
          if (data.error === null) {
            if (self.scope.uploadSuccessCallback) {
              self.scope.uploadSuccessCallback(data);
            }

            var errors = _.get(data, 'data.errors');
            setInfoType(self.scope.uploadInfos[index], errors, self.statesToClasses);
            // there might be warnings. display them
            if (_.isNotEmpty(errors)) {
              self.scope.uploadInfos[index].errors = errors;
            }
          } else {
            self.scope.uploadInfos[index].infoType = self.statesToClasses.error;
            self.handleUploadErrors(index, data);
          }
        }).error(function(data, status) {
          self.scope.uploadInfos[index].infoType = self.statesToClasses.error;
          self.scope.uploadInfos[index].error = {
            code: status,
            message: 'An Error has occurred on the server!'
          };
        });
      }
    };

    return function(scope) {
      return new FileUploadManager(scope);
    };
  }]);

  modules.get('a4c-common', ['ngFileUpload']).controller('UploadCtrl', [ '$scope', 'Upload', '$q', 'uploadServiceFactory', function($scope, $upload, $q, uploadServiceFactory) {

    if(_.defined($scope.getWorkspaceSpecifics) && _.isFunction($scope.getWorkspaceSpecifics())){
      $scope.getWorkspaceSpecifics()($scope);
    }

    var uploadService = uploadServiceFactory($scope);
    //build the upload directive data
    function buildUploadData(file) {
      var data = {file: file};

      //these can be simple values or functions
      var url = angular.isFunction($scope.targetUrl()) ? $scope.targetUrl()() : $scope.targetUrl();
      var requestData = angular.isFunction($scope.requestData()) ? $scope.requestData()($scope) : $scope.requestData();

      if(_.defined(url)){
        data.url = url;
      }
      if(_.defined(requestData)){
        data.data = requestData;
      }
      return data;
    }

    $scope.doUpload = function(file) {
      var uploadData =  buildUploadData(file);
      uploadService.doUpload(file, uploadData);
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
