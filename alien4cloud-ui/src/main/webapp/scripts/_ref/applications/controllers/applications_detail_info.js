define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');
  var angular = require('angular');
  var alienUtils = require('scripts/utils/alien_utils');
  var _ = require('lodash');

  require('scripts/layout/resource_layout');

  require('scripts/common/services/option_service');

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
    ['$controller', '$scope', '$state', '$window','$translate', 'toaster', 'Upload', 'menu', 'resourceLayoutService', 'authService', 'applicationServices', 'application', 'applicationEnvironmentsManager', 'archiveVersions', 'optionService',
    function ($controller, $scope, $state, $window, $translate, toaster, $upload, menu, resourceLayoutService, authService, applicationServices, applicationResponse, applicationEnvironmentsManager, versionsResponse, optionService) {
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


      var options = optionService.get("env_search_options");
      if (_.undefined(options) || _.undefined(options[$scope.application.id])) {
            if (_.undefined(options)) {
                options = {};
            }

            options[$scope.application.id] = {
                  filter: {},
                  column: 1,
                  descending: false
            };

            optionService.set("env_search_options",options);

            $scope.search_options = options[$scope.application.id];
      }
      $scope.search_options = options[$scope.application.id];

      $scope.changeFilter = function(env) {
        $scope.search_options.filter = env;
        optionService.set("env_search_options",options);
        console.log($scope.search_options);
      };

      $scope.sortClass = function(i) {
        if (i == $scope.search_options.column) {
            if ($scope.search_options.descending == true) {
                return "fa fa-sort-desc fa-fw";
            } else {
                return "fa fa-sort-asc fa-fw";
            }
        } else {
            return "fa fa-sort fa-fw text-muted";
        }
      };

      $scope.sortValue = function(e) {
        switch($scope.search_options.column) {
        case 1:
            return e.name;
        case 2:
            return e.environmentType;
        case 3:
            return e.currentVersionName;
        case 4:
            return e.deployedVersion;
        }
      };

      $scope.changeSort = function(i) {
        if (i == $scope.search_options.column) {
            $scope.search_options.descending = ! $scope.search_options.descending;
        } else {
            $scope.search_options.column = i;
            $scope.search_options.descending = false;
        }
        optionService.set("env_search_options",options);
      };

      $scope.onEnvironment = function (environmentId) {
        $state.go('applications.detail.environment', {
          environmentId: environmentId
        });
      };

      $scope.onEnvironmentInNewTab = function (environmentId) {
        var url = $state.href('applications.detail.environment', {
          environmentId: environmentId
        });
        console.log(url);
        $window.open(url,'_blank');
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
