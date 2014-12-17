/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('ApplicationVersionsCtrl', ['$scope', '$state', '$translate', 'toaster', 'alienAuthService', '$modal', 'applicationVersionServices', 'appVersions',
  function($scope, $state, $translate, toaster, alienAuthService, $modal, applicationVersionServices, appVersions) {
    $scope.isManager = alienAuthService.hasRole('APPLICATIONS_MANAGER');
    $scope.searchAppVersionResult = appVersions;
    $scope.query = "";

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
        });
      });
    };

    // Search for application Versions
    $scope.search = function() {
      var searchRequestObject = {
        'query': $scope.query,
        'from': 0,
        'size': 50
      };
      applicationVersionServices.searchVersions({
        applicationId: $scope.application.id
      }, angular.toJson(searchRequestObject), function updateAppVersionSearchResult(result) {
        $scope.searchAppVersionResult = result.data.data;
      });
    };

    // Delete the app environment
    $scope.delete = function deleteAppEnvironment(versionId) {
      if (!angular.isUndefined(versionId)) {
        applicationVersionServices.delete({
          applicationId: $scope.application.id,
          applicationVersionId: versionId
        }, null, function deleteAppEnvironment(result) {
          $scope.search();
        });
      }
    };

  }
]);

var NewApplicationVersionCtrl = ['$scope', '$modalInstance', '$resource', 'searchServiceFactory', '$state', 'applicationVersionServices',
  function($scope, $modalInstance, $resource, searchServiceFactory, $state, applicationVersionServices) {
    $scope.appVersion = {};

    $scope.save = function(appVersion) {
      applicationVersionServices.create({applicationId: $scope.application.id}, angular.toJson(appVersion)).$promise.then(function(success){
        $modalInstance.close(success.data);
      });
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
  }
];
