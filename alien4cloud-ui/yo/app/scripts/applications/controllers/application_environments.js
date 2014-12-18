/* global UTILS */
'use strict';

var NewApplicationEnvironmentCtrl = ['$scope', '$modalInstance', '$resource', 'searchServiceFactory', '$state', 'applicationEnvironmentServices', 'applicationVersionServices',
  function($scope, $modalInstance, $resource, searchServiceFactory, $state, applicationEnvironmentServices, applicationVersionServices) {

    // Created environment object
    $scope.environment = {};
    $scope.create = function(valid, cloudId, envType, version) {
      if (valid) {
        // prepare the good request
        var applicationId = $state.params.id;
        $scope.environment.cloudId = cloudId;
        $scope.environment.applicationId = applicationId;
        $scope.environment.environmentType = envType;
        $scope.environment.versionId = version;
        $modalInstance.close($scope.environment);
      }
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    var searchVersions = function () {
      // recover all versions for this applications
      var searchRequestObject = {
        'query': '',
        'from': 0,
        'size': 50
      };
      applicationVersionServices.searchVersion({
        applicationId: $state.params.id
      }, angular.toJson(searchRequestObject), function versionSearchResult(result) {
        // Result search
        $scope.versions = result.data.data;
      });

    };
    searchVersions();

    // Cloud search to configure the new environment
    $scope.query = '';
    $scope.onSearchCompleted = function(searchResult) {
      $scope.clouds = searchResult.data.data;
    };
    $scope.searchService = searchServiceFactory('rest/clouds/search', true, $scope, 50);

    $scope.search = function() {
      $scope.searchService.search();
    };

    // first load
    $scope.search();

    // Get environment type list (cached value)
    $scope.envTypeList = applicationEnvironmentServices.environmentTypeList({}, {}, function(successResponse) {});

  }
];

angular.module('alienUiApp').controller('ApplicationEnvironmentsCtrl', ['$scope', '$state', '$translate', 'toaster', 'alienAuthService', '$modal', 'applicationEnvironmentServices', 'environments',
  function($scope, $state, $translate, toaster, alienAuthService, $modal, applicationEnvironmentServices, environments) {

    $scope.isManager = alienAuthService.hasRole('APPLICATIONS_MANAGER');

    // Initial scope environment loaded from parent state : applications.details
    $scope.searchAppEnvResult = environments;

    // Modal to create an new application environment
    $scope.openNewAppEnv = function() {
      var modalInstance = $modal.open({
        templateUrl: 'newApplicationEnvironment.html',
        controller: NewApplicationEnvironmentCtrl
      });
      modalInstance.result.then(function(environment) {
        // create a new application environment from the given name, description and cloud
        applicationEnvironmentServices.create({
          applicationId: $scope.application.id
        }, angular.toJson(environment), function(successResponse) {
          $scope.search();
        });
      });
    };

    // Search for application environments
    $scope.search = function() {
      var searchRequestObject = {
        'query': $scope.query,
        'from': 0,
        'size': 50
      };
      applicationEnvironmentServices.searchEnvironment({
        applicationId: $scope.application.id
      }, angular.toJson(searchRequestObject), function updateAppEnvSearchResult(result) {
        // Result search
        $scope.searchAppEnvResult = result.data.data;
      });
      // TODO : UPDATE env status ?
      // // when apps search result is ready, update apps statuses
      // searchResult.$promise.then(function(applisationListResult) {
      //   updateApplicationStatuses(applisationListResult);
      // });
    };


    // Delete the app environment
    $scope.delete = function deleteAppEnvironment(appEnvId) {
      console.log('Delete the appEnvID : ', appEnvId);
      if (!angular.isUndefined(appEnvId)) {
        applicationEnvironmentServices.delete({
          applicationId: $scope.application.id,
          applicationEnvironmentId: appEnvId
        }, null, function deleteAppEnvironment(result) {
          // Result search
          $scope.search();
        });
      }
    };

  }
]);
