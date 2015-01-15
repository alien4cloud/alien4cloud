/* global UTILS */
'use strict';

var NewApplicationEnvironmentCtrl = ['$scope', '$modalInstance', '$resource', '$state',
  function($scope, $modalInstance, $resource, $state) {
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
  }
];

angular.module('alienUiApp').controller('ApplicationEnvironmentsCtrl', ['$scope', '$state', '$translate', 'toaster', 'alienAuthService', '$modal', 'applicationEnvironmentServices', '$rootScope', '$resolve', 'applicationVersionServices', 'searchServiceFactory',
  function($scope, $state, $translate, toaster, alienAuthService, $modal, applicationEnvironmentServices, $rootScope, $resolve, applicationVersionServices, searchServiceFactory) {

    $scope.isManager = alienAuthService.hasRole('APPLICATIONS_MANAGER');
    $scope.envTypeList = applicationEnvironmentServices.environmentTypeList({}, {}, function(successResponse) {});

    // Application versions search
    var searchVersions = function() {
      var searchRequestObject = {
        'query': '',
        'from': 0,
        'size': 50
      };
      applicationVersionServices.searchVersion({
        applicationId: $state.params.id
      }, angular.toJson(searchRequestObject), function versionSearchResult(result) {
        $scope.versions = result.data.data;
      });

    };
    searchVersions();

    // Cloud search
    $scope.onSearchCompleted = function(searchResult) {
      $scope.clouds = searchResult.data.data;
    };
    $scope.searchService = searchServiceFactory('rest/clouds/search', true, $scope, 50);
    $scope.searchClouds = function() {
      $scope.searchService.search();
    };
    $scope.searchClouds();

    // Modal to create an new application environment
    $scope.openNewAppEnv = function() {
      var modalInstance = $modal.open({
        templateUrl: 'newApplicationEnvironment.html',
        controller: NewApplicationEnvironmentCtrl,
        scope: $scope
      });
      modalInstance.result.then(function(environment) {
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
      return applicationEnvironmentServices.searchEnvironment({
        applicationId: $scope.application.id
      }, angular.toJson(searchRequestObject), function updateAppEnvSearchResult(result) {
        $scope.searchAppEnvResult = result.data.data;
        return $scope.searchAppEnvResult;
      }).$promise;
    };
    $scope.search();

    // Delete the app environment
    $scope.delete = function deleteAppEnvironment(appEnvId) {
      if (!angular.isUndefined(appEnvId)) {
        applicationEnvironmentServices.delete({
          applicationId: $scope.application.id,
          applicationEnvironmentId: appEnvId
        }, null, function deleteAppEnvironment(result) {
          $scope.search();
        });
      }
    };

    var getVersionIdByName = function(name) {
      for (var i = 0; i < $scope.versions.length; i++) {
        if ($scope.versions[i].version === name) {
          return $scope.versions[i].id;
        }
      }
    }

    var getCloudIdByName = function(name) {
      for (var i = 0; i < $scope.clouds.length; i++) {
        if ($scope.clouds[i].name === name) {
          return $scope.clouds[i].id;
        }
      }
    }

    $scope.updateApplicationEnvironment = function(fieldName, fieldValue, environmentId, oldValue) {
      if (fieldName !== 'name' || fieldValue !== oldValue) {
        var updateApplicationEnvironmentRequest = {};

        if (fieldName === 'currentVersionId') {
          updateApplicationEnvironmentRequest[fieldName] = getVersionIdByName(fieldValue);
        } else if (fieldName === 'cloudId') {
          updateApplicationEnvironmentRequest[fieldName] = getCloudIdByName(fieldValue);
        } else {
          updateApplicationEnvironmentRequest[fieldName] = fieldValue;
        }

        return applicationEnvironmentServices.update({
          applicationId: $scope.application.id,
          applicationEnvironmentId: environmentId,
        }, angular.toJson(updateApplicationEnvironmentRequest), undefined).$promise.then({},
          function(errorResponse) {
            return $translate('ERRORS.' + errorResponse.data.error.code);
          }
        );
      }
    };

  }
]);
