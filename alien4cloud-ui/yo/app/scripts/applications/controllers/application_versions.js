'use strict';

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

angular.module('alienUiApp').controller('ApplicationVersionsCtrl', ['$scope', '$state', '$translate', 'toaster', 'alienAuthService', '$modal', 'applicationVersionServices', 'appVersions', 'searchServiceFactory',
  function($scope, $state, $translate, toaster, alienAuthService, $modal, applicationVersionServices, appVersions, searchServiceFactory) {
    $scope.isManager = alienAuthService.hasRole('APPLICATIONS_MANAGER');
    $scope.appVersions = appVersions.data;
    $scope.searchAppVersionResult = appVersions.data;
    $scope.versionPattern = new RegExp('^\\d+(?:\\.\\d+)*(?:[a-zA-Z0-9\\-_]+)*$');

    $scope.searchService = searchServiceFactory('rest/applications/' + $scope.application.id + '/versions/search', false, $scope, 12);
    $scope.searchService.search();
    $scope.onSearchCompleted = function(searchResult) {
      $scope.searchAppVersionResult = searchResult.data.data;
    };

    var refreshAllAppVersions = function() {
      var searchAppVersionRequestObject = {
        'from': 0,
        'size': 400
      };
      applicationVersionServices.searchVersion({
        applicationId: $scope.application.id
      }, angular.toJson(searchAppVersionRequestObject)).$promise.then(function(result) {
        appVersions.data = result.data.data;
        $scope.appVersions = appVersions.data;
      });
    };

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
          $scope.searchService.search();
          refreshAllAppVersions();
        });
      });
    };

    $scope.delete = function deleteAppEnvironment(versionId) {
      if (!angular.isUndefined(versionId)) {
        applicationVersionServices.delete({
          applicationId: $scope.application.id,
          applicationVersionId: versionId
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
      return applicationVersionServices.update({
        applicationId: $scope.application.id,
        applicationVersionId: versionId
      }, angular.toJson(applicationVersionUpdateRequest), undefined).$promise.then(
        function() {}, function(errorResponse) {
          return $translate('ERRORS.' + errorResponse.data.error.code);
        }
      );
    };


  }
]);
