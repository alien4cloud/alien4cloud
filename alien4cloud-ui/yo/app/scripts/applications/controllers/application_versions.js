/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('ApplicationVersionsCtrl', ['$scope', '$state', '$translate', 'toaster', 'alienAuthService', '$modal', 'applicationVersionServices', 'appVersions',
  function($scope, $state, $translate, toaster, alienAuthService, $modal, applicationVersionServices, appVersions) {
    $scope.isManager = alienAuthService.hasRole('APPLICATIONS_MANAGER');
    $scope.appVersions = appVersions;
    $scope.searchAppVersionResult = appVersions;

    $scope.openNewAppVersion = function() {
      var modalInstance = $modal.open({
        templateUrl: 'newApplicationVersion.html',
        controller: NewApplicationVersionCtrl,
        scope: $scope
      });
      modalInstance.result.then(function(appVersion) {
        applicationVersionServices.create({
          applicationId: $scope.application.id
        }, angular.toJson(appVersion), function(successResponse) {
          $scope.search();
          appVersions.push(appVersion);
        });
      });
    };

    $scope.search = function() {
      var searchRequestObject = {
        'query': $scope.query,
        'from': 0,
        'size': 50
      };
      applicationVersionServices.searchVersion({
        applicationId: $scope.application.id
      }, angular.toJson(searchRequestObject), function updateAppVersionSearchResult(result) {
        $scope.searchAppVersionResult = result.data.data;
      });
    };

    $scope.delete = function deleteAppEnvironment(versionId) {
      if (!angular.isUndefined(versionId)) {
        applicationVersionServices.delete({
          applicationId: $scope.application.id,
          applicationVersionId: versionId
        }, null, function deleteAppEnvironment(result) {
          $scope.search();
          for (var i=0; i<appVersions.length; i++) {
            if (appVersions[i].id === versionId) {
              appVersions.splice(i, 1);
              break;
            }
          }
        });
      }
    };

  }
]);

var NewApplicationVersionCtrl = ['$scope', '$modalInstance', '$resource', 'searchServiceFactory', '$state', 'applicationVersionServices',
  function($scope, $modalInstance, $resource, searchServiceFactory, $state, applicationVersionServices) {
    $scope.appVersion = {};

    $scope.create = function(valid, version, desc, oldAppVersion) {
      if (valid) {
        $scope.appVersion.version = version;
        $scope.appVersion.description = desc;
        if (oldAppVersion) {
          $scope.appVersion.topologyId = oldAppVersion.topologyId;
        }
        $modalInstance.close($scope.appVersion);
      }
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
  }
];
