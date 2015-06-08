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

angular.module('alienUiApp').controller('ApplicationVersionsCtrl', ['$scope', '$state', '$translate', 'toaster', 'alienAuthService', '$modal', 'versionServices', 'appVersions', 'searchServiceFactory', 'searchServiceUrl', 'delegateId', 'userCanModify', 
  function($scope, $state, $translate, toaster, alienAuthService, $modal, versionServices, appVersions, searchServiceFactory, searchServiceUrl, delegateId, userCanModify) {
    $scope.isManager = userCanModify;
    $scope.appVersions = appVersions.data;
    $scope.searchAppVersionResult = appVersions.data;
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
        versionServices.create({
          delegateId: delegateId
        }, angular.toJson(appVersion), function(successResponse) {
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
          return $translate('ERRORS.' + errorResponse.data.error.code);
        }
      );
    };


  }
]);
