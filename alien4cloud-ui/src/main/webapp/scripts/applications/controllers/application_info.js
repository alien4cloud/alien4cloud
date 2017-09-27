define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');
  var alienUtils = require('scripts/utils/alien_utils');

  require('scripts/applications/services/application_services');
  require('scripts/applications/services/application_environment_services');
  require('scripts/applications/services/application_version_services');
  require('scripts/meta-props/directives/meta_props_display');

  require('scripts/deployment/directives/display_outputs');

  states.state('applications.detail.info', {
    url: '/infos',
    templateUrl: 'views/applications/application_infos.html',
    controller: 'ApplicationInfosCtrl',
    menu: {
      id: 'am.applications.info',
      state: 'applications.detail.info',
      key: 'NAVAPPLICATIONS.MENU_INFO',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationInfosCtrl',
    ['$scope', '$state', 'authService', 'Upload', '$translate', 'applicationServices', 'suggestionServices', 'toaster', 'application', 'appEnvironments',
    function($scope, $state, authService, $upload, $translate, applicationServices, suggestionServices,  toaster, applicationResult, appEnvironments) {
      $scope.applicationServices = applicationServices;
      $scope.fromStatusToCssClasses = alienUtils.getStatusIconCss;

      /* Tag name with all letters a-Z and - and _ and no space */
      $scope.tagKeyPattern = /^[\-\w\d_]*$/;
      $scope.application = applicationResult.data;
      var pageStateId = $state.current.name;

      $scope.isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
      $scope.isDeployer = authService.hasResourceRole($scope.application, 'APPLICATION_DEPLOYER');
      $scope.isDevops = authService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
      $scope.isUser = authService.hasResourceRole($scope.application, 'APPLICATION_USER');
      $scope.newAppName = $scope.application.name;

      $scope.isAllowedModify = _.defined($scope.application.topologyId) && ($scope.isManager || $scope.isDevops);
      $scope.appEnvironments = appEnvironments;

      $scope.getTabIndex = function (environmentId) {
        var envs = appEnvironments.environments;
        if (_.defined(envs)) {
          for (var i = 0, len = envs.length; i < len; i++) {
            if (envs[i].id === environmentId) {
              return i;
            }
          }
        }
        return -1;
      };

      $scope.setEnvironment = function setEnvironment(environmentId) {
        if (_.undefined(environmentId)) {
          environmentId = $scope.selectedEnvironment.id;
        }

        // select an environment and register a callback in case the env has changed.
        appEnvironments.select(environmentId, function() {
          $scope.selectedEnvironment = appEnvironments.selected;
          $scope.stopEvent(); // stop to listen for instance events
          delete $scope.deployedContext.dto;
          if (appEnvironments.selected.status !== 'UNDEPLOYED') {
            // If the application is deployed then get informations to display.
            $scope.processDeploymentTopologyInformation().$promise.then(function() {
              $scope.refreshInstancesStatuses($scope.application.id, appEnvironments.selected.id, pageStateId);
            });
          }
        }, true);
      };

      if (_.defined(appEnvironments.selected)) {
        $scope.setEnvironment(appEnvironments.selected.id);
      }

      $scope.$on('$destroy', function() { // routing to another page
        $scope.stopEvent(); // stop to listen for instance events
      });

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
