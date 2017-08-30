define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');
  var alienUtils = require('scripts/utils/alien_utils');

  states.state('applications.detail.environment.deploycurrent.info', {
    url: '/info',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_info.html',
    controller: 'ApplicationEnvDeployCurrentInfoCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.info',
      state: 'applications.detail.environment.deploycurrent.info',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.INFO',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentInfoCtrl',
  ['$scope', '$state', 'authService', 'Upload', '$translate', 'applicationServices', 'suggestionServices', 'toaster', 'application',
  function($scope, $state, authService, $upload, $translate, applicationServices, suggestionServices,  toaster, applicationResult) {
    $scope.applicationServices = applicationServices;
    $scope.fromStatusToCssClasses = alienUtils.getStatusIconCss;

    /* Tag name with all letters a-Z and - and _ and no space */
    $scope.tagKeyPattern = /^[\-\w\d_]*$/;
    $scope.application = applicationResult.data;

    $scope.isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    $scope.isDeployer = authService.hasResourceRole($scope.application, 'APPLICATION_DEPLOYER');
    $scope.isDevops = authService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
    $scope.isUser = authService.hasResourceRole($scope.application, 'APPLICATION_USER');
    $scope.newAppName = $scope.application.name;

    $scope.isAllowedModify = _.defined($scope.application.topologyId) && ($scope.isManager || $scope.isDevops);

    // Image upload
    $scope.doUpload = function(file) {
      $upload.upload({
        url: 'rest/latest/applications/' + $scope.application.id + '/image',
        file: file
      }).success(function(result) {
        $scope.application.imageId = result.data;
      });
    };
    $scope.onImageSelected = function($files) {
      var file = $files[0];
      $scope.doUpload(file);
    };      
  }
]);
});
