define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');
  var angular = require('angular');
  var alienUtils = require('scripts/utils/alien_utils');
  var _ = require('lodash');

  require('scripts/layout/resource_layout');

  require('scripts/_ref/applications/controllers/applications_detail_users');
  require('scripts/_ref/applications/controllers/applications_detail_versions');
  require('scripts/_ref/applications/controllers/applications_detail_environments');
  require('scripts/_ref/applications/controllers/applications_detail_variables');

  require('scripts/_ref/applications/controllers/applications_detail_environment');
  require('scripts/_ref/applications/controllers/applications_detail_version');

  require('scripts/_ref/applications/services/application_environments_manager_factory');

  require('scripts/applications/services/application_services');
  require('scripts/applications/services/application_version_services');

  require('scripts/_ref/common/directives/tags');
  require('scripts/_ref/common/filters/highlight');
  require('scripts/meta-props/directives/meta_props_display');

  states.state('applications.detail.info', {
    url: '/infos',
    templateUrl: 'views/_ref/applications/applications_detail_info.html',
    controller: 'ApplicationInfoCtrl'
  });
  states.forward('applications.detail', 'applications.detail.info');

  modules.get('a4c-applications').controller('ApplicationInfoCtrl',
    ['$controller', '$scope', '$state', '$translate', 'toaster', 'Upload', 'menu', 'resourceLayoutService', 'authService', 'applicationServices', 'application', 'applicationEnvironmentsManager', 'archiveVersions',
    function ($controller, $scope, $state, $translate, toaster, $upload, menu, resourceLayoutService, authService, applicationServices, applicationResponse, applicationEnvironmentsManager, versionsResponse) {
      $scope.application = applicationResponse.data;

      $scope.versions = versionsResponse.data;

      if(!$scope.application.tags) {
        $scope.application.tags = {};
      }
      $scope.environments = applicationEnvironmentsManager.environments;
      $scope.statusCss = alienUtils.getStatusIconCss;

      // Add the resource layout controller to the scope (mixin)
      $controller('ResourceLayoutCtrl', {$scope: $scope, menu: menu, resourceLayoutService: resourceLayoutService, resource: $scope.application});

      // Application rights
      $scope.isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');

      $scope.$on('$destroy', function() {
        // We must stop all event registrations
        applicationEnvironmentsManager.stopEvents();
      });

      $scope.onEnvironment = function (environmentId) {
        $state.go('applications.detail.environment', {
          environmentId: environmentId
        });
      };

      $scope.onVersion = function (versionId) {
        $state.go('applications.detail.version', {
          versionId: versionId
        });
      };

      $scope.removeApplication = function(applicationId) {
        applicationServices.remove({
          applicationId: applicationId
        }, function(response) {
          if (!response.error && response.data === true) {
            $state.go('applications.list');
          } else {
            // toaster message
            toaster.pop('error', $translate.instant('APPLICATIONS.ERRORS.DELETE_TITLE'), $translate.instant('APPLICATIONS.ERRORS.DELETING_FAILED'), 4000, 'trustedHtml', null);
          }
        });
      };

      $scope.updateApplication = function(fieldName, fieldValue) {
        var applicationUpdateRequest = {};
        if(_.undefined(fieldValue)) {
          fieldValue = '';
        }
        applicationUpdateRequest[fieldName] = fieldValue;
        return applicationServices.update({
          applicationId: $scope.application.id
        }, angular.toJson(applicationUpdateRequest), undefined).$promise.then(
          function() { // Success
            // reload the current application info page after update
            $state.go($state.current, {}, {reload: true});
          },
          function(errorResponse) { // Error
            return $translate.instant('ERRORS.' + errorResponse.data.error.code);
          }
        );
      };

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
