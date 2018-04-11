define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

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

  var NewApplicationVersionCtrl = ['$scope', '$uibModalInstance',
  function($scope, $uibModalInstance) {
    $scope.selectTopoVersion = false;
    $scope.appVersion = {};
    $scope.fromIndex = 1;
    $scope.create = function(valid) {
      if (valid) {
        if($scope.fromIndex === 1) { // create from previous version
          $scope.appVersion.fromVersionId = $scope.fromVersion.selected.id;
        } else if($scope.fromIndex === 2) { // create from template
          $scope.appVersion.topologyTemplateId = $scope.topologyTemplate.versionId;
        }
        $uibModalInstance.close($scope.appVersion);
      }
    };
    $scope.cancel = function() {
      $uibModalInstance.dismiss('cancel');
    };
  }];

  var NewApplicationTopologyVersionCtrl = ['$scope', '$uibModalInstance',
  function($scope, $uibModalInstance) {
    $scope.selectTopoVersion = true;
    //regex:
    // - do not allow 'snapshot' string,
    //- allow all alphanumeric char
    //- "i" ==>ignore case

    $scope.qualifierPattern = new RegExp('^((?!.*snapshot)[a-zA-Z0-9\\-_]+)*$', 'i');
    $scope.appTopoVersion = {};
    $scope.fromIndex = 1;
    // NOTE /// This is only for visual. The generated topology version is not actually sent to the server.
    // NOTE /// So Make sure to match what is generated here with what is done on the backend side
    var prefix = $scope.selectedVersion.version;
    var suffix = '';
    var qualifierIndex = $scope.selectedVersion.version.indexOf('-');
    if(qualifierIndex > 0) {
      prefix = $scope.selectedVersion.version.substring(0, qualifierIndex);
      suffix = $scope.selectedVersion.version.substring(qualifierIndex, $scope.selectedVersion.version.length);
    }

    $scope.$watch('appTopoVersion.qualifier', function() {
      if(_.defined($scope.appTopoVersion.qualifier) && $scope.appTopoVersion.qualifier !== '') {
        $scope.computedVersion = prefix + '-' + $scope.appTopoVersion.qualifier + suffix;
      } else {
        $scope.computedVersion = $scope.selectedVersion.version;
      }
    });
    $scope.create = function(valid) {
      if (valid) {
        if($scope.fromIndex === 1) { // create from previous version
          $scope.appTopoVersion.applicationTopologyVersion = $scope.fromVersion.selected.topologyVersions[$scope.fromVersion.selectedTopoVersion].archiveId;
        } else if($scope.fromIndex === 2) { // create from template
          $scope.appTopoVersion.topologyTemplateId = $scope.topologyTemplate.versionId;
        }
        $uibModalInstance.close($scope.appTopoVersion);
      }
    };
    $scope.cancel = function() {
      $uibModalInstance.dismiss('cancel');
    };
  }];

  modules.get('a4c-applications').controller('ApplicationVersionsCtrl', ['$scope', '$translate', '$uibModal', '$alresource', 'userContextServices', 'versionServices', 'archiveVersions', 'searchServiceFactory', 'searchServiceUrl', 'delegateId', 'userCanModify', 'appEnvironments',
    function($scope, $translate, $uibModal, $alresource, userContextServices, versionServices, archiveVersions, searchServiceFactory, searchServiceUrl, delegateId, userCanModify, appEnvironments) {
      var topoVersionService = $alresource('rest/latest/applications/:appId/versions/:versionId/topologyVersions/:topoVersionId');

      $scope.isManager = userCanModify;
      $scope.archiveVersions = archiveVersions.data;
      $scope.appEnvironments = appEnvironments;
      $scope.searchAppVersionResult = archiveVersions.data;
      $scope.versionPattern = versionServices.pattern;
      $scope.searchService = searchServiceFactory(searchServiceUrl, false, $scope, 12);
      $scope.searchService.search();
      $scope.onSearchCompleted = function(searchResult) {
        $scope.searchAppVersionResult = searchResult.data.data;
      };

      var refreshAllAppVersions = function() {
        var searchAppVersionRequestObject = {
          'from': 0,
          'size': 1000
        };
        versionServices.searchVersion({
          delegateId: delegateId
        }, angular.toJson(searchAppVersionRequestObject)).$promise.then(function(result) {
          archiveVersions.data = result.data.data;
          $scope.archiveVersions = archiveVersions.data;
        });
      };

      $scope.openNewAppVersion = function() {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/applications/application_version_new.html',
          controller: NewApplicationVersionCtrl,
          scope: $scope,
          windowClass: 'new-app-modal'
        });
        modalInstance.result.then(function(createAppVersionRequest) {
          versionServices.create({
            delegateId: delegateId
          }, angular.toJson(createAppVersionRequest), function() {
            $scope.searchService.search();
            refreshAllAppVersions();
          });
        });
      };

      $scope.delete = function(versionId) {
        if (!angular.isUndefined(versionId)) {
          versionServices.delete({
            delegateId: delegateId,
            versionId: versionId
          }, null, function(result) {
            if (result) {
              $scope.searchService.search();
              refreshAllAppVersions();
            }
          });
        }
      };

      $scope.updateApplicationVersion = function(fieldName, fieldValue, version) {
        var applicationVersionUpdateRequest = {};
        applicationVersionUpdateRequest[fieldName] = fieldValue;
        return versionServices.update({
          delegateId: delegateId,
          versionId: version.id
        }, angular.toJson(applicationVersionUpdateRequest), undefined).$promise.then(
          function() {
            // success
            var previousTopologiesContext = userContextServices.getTopologyContext(version.applicationId);
            if (fieldName === 'version') {
              _.each($scope.appEnvironments.environments, function(env) {
                  if (env.applicationId === version.applicationId && env.currentVersionName === version.version) {
                    env.currentVersionName = fieldValue;
                    return;
                  }
                });
              //emit an event to the parent scopes. This is for example to refresh the environments
              $scope.$emit('applicationVersionChanged', version);
            }
            $scope.searchService.search();
            refreshAllAppVersions();
            if (_.defined(previousTopologiesContext)) {
              userContextServices.deleteTopologyContext(version.applicationId);
            }
          }, function() {
            return false;
          }
        );
      };

      $scope.openNewAppTopoVersion = function(version) {
        $scope.selectedVersion = version;
        var modalInstance = $uibModal.open({
          templateUrl: 'views/applications/application_version_topology_new.html',
          controller: NewApplicationTopologyVersionCtrl,
          scope: $scope,
          windowClass: 'new-app-modal'
        });
        modalInstance.result.then(function(createAppTopoVersionRequest) {
          topoVersionService.create({
            appId: delegateId,
            versionId: version.id
          }, angular.toJson(createAppTopoVersionRequest), function() {
            $scope.searchService.search();
            refreshAllAppVersions();
          });
        });
      };

      $scope.deleteAppTopoVersion = function(version, topoVersionId) {
        topoVersionService.delete({
          appId: delegateId,
          versionId: version.id,
          topoVersionId: topoVersionId
        }, null, function() {
          $scope.searchService.search();
          refreshAllAppVersions();
        });
      };
    }
  ]);
}); // define
