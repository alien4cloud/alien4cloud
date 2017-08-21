/*
* Application list is the entry point for the application management.
*/
define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');
  var angular = require('angular');
  var alienUtils = require('scripts/utils/alien_utils');
  var _ = require('lodash');

  require('scripts/applications/controllers/application_versions');
  require('scripts/applications/controllers/application_environments');
  require('scripts/applications/controllers/application_users');

  require('scripts/_ref/applications/controllers/applications_detail_environment');

  require('scripts/applications/services/application_environment_builder');
  require('scripts/applications/services/application_services');
  require('scripts/applications/services/application_version_services');

  require('scripts/_ref/common/directives/tags');
  require('scripts/common/services/user_context_services');

  require('scripts/meta-props/directives/meta_props_display');

  require('scripts/layout/layout');

  states.state('applications.detail', {
    url: '/detail/:id',
    template: '<ui-view/>',
    // templateUrl: 'views/_ref/applications/applications_detail.html',
    // controller: 'ApplicationDetailCtrl',
    resolve: {
      application: ['applicationServices', '$stateParams',
        function(applicationServices, $stateParams) {
          return applicationServices.get({
            applicationId: $stateParams.id
          }).$promise;
        }
      ],
      appEnvironments: ['application', 'appEnvironmentsBuilder', '$stateParams','userContextServices',
        function(application, appEnvironmentsBuilder, $stateParams, userContextServices) {
          return appEnvironmentsBuilder(application.data, $stateParams.openOnEnvironment, userContextServices);
        }
      ],
      archiveVersions: ['$http', 'application', 'applicationVersionServices',
        function($http, application, applicationVersionServices) {
          var searchAppVersionRequestObject = {
            'from': 0,
            'size': 400
          };
          return applicationVersionServices.searchVersion({
            delegateId: application.data.id
          }, angular.toJson(searchAppVersionRequestObject)).$promise.then(function(result) {
            return result.data;
          });
        }
      ]
    },
    params: {
      // optional id of the environment to automatically select when triggering this state
      openOnEnvironment: null
    }
  });

  states.state('applications.detail.info', {
    url: '/infos',
    templateUrl: 'views/_ref/applications/applications_detail.html',
    controller: 'ApplicationDetailCtrl'
  });
  states.forward('applications.detail', 'applications.detail.info');

  modules.get('a4c-applications').controller('ApplicationDetailCtrl',
    ['$scope', '$state', '$translate', 'toaster', 'Upload', 'menu', 'layoutService', 'authService', 'applicationServices', 'application', 'appEnvironments', 'archiveVersions',
    function ($scope, $state, $translate, toaster, $upload, menu, layoutService, authService, applicationServices, applicationResponse, environments, versions) {
      $scope.application = applicationResponse.data;
      if(!$scope.application.tags) {
        $scope.application.tags = {};
      }
      $scope.environments = environments.environments;
      $scope.statusCss = alienUtils.getStatusCss;

      layoutService.process(menu);
      $scope.menu = menu;
      $scope.onItemClick = function($event, menuItem) {
        if (menuItem.disabled) {
          $event.preventDefault();
          $event.stopPropagation();
        }
      };

      // Application rights
      $scope.isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');

      $scope.onEnvironment = function (environmentId) {
        $state.go('applications.detail.environment', {
          environmentId: environmentId
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
