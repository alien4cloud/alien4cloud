define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/common/directives/pagination');

  states.state('applications.detail.versions', {
    url: '/versions',
    templateUrl: 'views/applications/application_versions.html',
    controller: 'ApplicationVersionsCtrl',
    resolve: {
      versionServices: ['applicationVersionServices', function(applicationVersionServices) { return applicationVersionServices; }],
      searchServiceUrl: ['application', function(application) { return 'rest/latest/applications/' + application.data.id + '/versions/search'; }],
      delegateId: ['application', function(application) { return application.data.id; }],
      userCanModify: ['authService', function(authService) { return authService.hasRole('APPLICATIONS_MANAGER'); }]
    },
    menu: {
      id: 'am.applications.detail.versions',
      state: 'applications.detail.versions',
      key: 'NAVAPPLICATIONS.MENU_VERSIONS',
      icon: 'fa fa-tasks',
      roles: ['APPLICATION_MANAGER'],
      priority: 500
    }
  });

  var NewApplicationVersionCtrl = ['$scope', '$modalInstance',
  function($scope, $modalInstance) {
    $scope.appVersion = {};

    $scope.create = function(version, desc, oldAppVersion) {
      $scope.appVersion.version = version;
      $scope.appVersion.description = desc;
      if (oldAppVersion) {
        $scope.appVersion.topologyId = oldAppVersion.topologyId;
      }
      $modalInstance.close($scope.appVersion);
      $scope.searchService.search();
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
  }
  ];

  modules.get('a4c-applications').controller('ApplicationVersionsCtrl', ['$scope', '$state', '$translate', 'toaster', 'authService', '$modal', 'versionServices', 'archiveVersions', 'searchServiceFactory', 'searchServiceUrl', 'delegateId', 'userCanModify',
    function($scope, $state, $translate, toaster, authService, $modal, versionServices, archiveVersions, searchServiceFactory, searchServiceUrl, delegateId, userCanModify) {
      $scope.isManager = userCanModify;
      $scope.archiveVersions = archiveVersions.data;
      $scope.searchAppVersionResult = archiveVersions.data;
      $scope.versionPattern = new RegExp('^\\d+(?:\\.\\d+)*(?:[a-zA-Z0-9\\-_]+)*$');

      $scope.searchService = searchServiceFactory(searchServiceUrl, false, $scope, 12);
      $scope.searchService.search();
      $scope.onSearchCompleted = function(searchResult) {
        $scope.searchAppVersionResult = searchResult.data.data;
      };

      var refreshAllAppVersions = function() {
        var searchAppVersionRequestObject = {
          'from': 0,
          'size': 400
        };
        versionServices.searchVersion({
          delegateId: delegateId
        }, angular.toJson(searchAppVersionRequestObject)).$promise.then(function(result) {
          archiveVersions.data = result.data.data;
          $scope.archiveVersions = archiveVersions.data;
        });
      };

      $scope.openNewAppVersion = function() {
        var modalInstance = $modal.open({
          templateUrl: 'newApplicationVersion.html',
          controller: NewApplicationVersionCtrl,
          scope: $scope
        });
        modalInstance.result.then(function(appVersion) {
          versionServices.create({
            delegateId: delegateId
          }, angular.toJson(appVersion), function() {
            $scope.searchService.search();
            refreshAllAppVersions();
          });
        });
      };

      $scope.delete = function deleteAppEnvironment(versionId) {
        if (!angular.isUndefined(versionId)) {
          versionServices.delete({
            delegateId: delegateId,
            versionId: versionId
          }, null, function deleteAppEnvironment(result) {
            if (result) {
              $scope.searchService.search();
              refreshAllAppVersions();
            }
          });
        }
      };

      $scope.updateApplicationVersion = function(fieldName, fieldValue, versionId) {
        var applicationVersionUpdateRequest = {};
        applicationVersionUpdateRequest[fieldName] = fieldValue;
        return versionServices.update({
          delegateId: delegateId,
          versionId: versionId
        }, angular.toJson(applicationVersionUpdateRequest), undefined).$promise.then(
          function() {}, function(errorResponse) {
            return $translate.instant('ERRORS.' + errorResponse.data.error.code);
          }
        );
      };
    }
  ]);
}); // define
