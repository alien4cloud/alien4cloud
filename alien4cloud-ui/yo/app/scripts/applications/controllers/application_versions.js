/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('ApplicationVersionsCtrl', ['$scope', '$state', '$translate', 'toaster', 'alienAuthService', '$modal', 'applicationVersionServices',
  function($scope, $state, $translate, toaster, alienAuthService, $modal, applicationVersionServices) {

    $scope.isManager = alienAuthService.hasRole('APPLICATIONS_MANAGER');
    console.log('VersionsSSS');

    $scope.openNewAppEnv = function() {
      var modalInstance = $modal.open({
        templateUrl: 'newApplicationEnvironment.html',
        controller: NewApplicationEnvironmentCtrl
      });
      modalInstance.result.then(function(environment) {
        // create a new application environment from the given name, description and cloud
        applicationVersionservices.create({
          applicationId: $scope.application.id
        }, angular.toJson(environment), function(successResponse) {
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
      applicationVersionservices.searchEnvironment({
        applicationId: $scope.application.id
      }, angular.toJson(searchRequestObject), function updateAppEnvSearchResult(result) {
        $scope.searchAppEnvResult = result.data.data;
      });
    };

    // Delete the app environment
    $scope.delete = function deleteAppEnvironment(appEnvId) {
      console.log('Delete the appEnvID : ', appEnvId);
      if (!angular.isUndefined(appEnvId)) {
        applicationVersionservices.delete({
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



// var NewApplicationEnvironmentCtrl = ['$scope', '$modalInstance', '$resource', 'searchServiceFactory', '$state', 'applicationVersionservices', 'applicationVersionservices',
//   function($scope, $modalInstance, $resource, searchServiceFactory, $state, applicationVersionservices, applicationVersionservices) {
//
//     // Created environment object
//     $scope.environment = {};
//     $scope.create = function(valid, cloudId, envType, version) {
//       if (valid) {
//         if (!angular.isUndefined(cloudId)) {
//           var applicationId = $state.params.id;
//           $scope.environment.cloudId = cloudId;
//           $scope.environment.applicationId = applicationId;
//           $scope.environment.environmentType = envType;
//           $scope.environment.versionId = version;
//         }
//         $modalInstance.close($scope.environment);
//       }
//     };
//
//     $scope.cancel = function() {
//       $modalInstance.dismiss('cancel');
//     };
//
//     // recover all Versions for this applications
//     var searchRequestObject = {
//       'query': '',
//       'from': 0,
//       'size': 50
//     };
//     applicationVersionservices.searchVersion({
//       applicationId: $state.params.id
//     }, angular.toJson(searchRequestObject), function VersionsearchResult(result) {
//       // Result search
//       $scope.Versions = result.data.data;
//     });
//
//     // Cloud search to configure the new environment
//     $scope.query = '';
//     $scope.onSearchCompleted = function(searchResult) {
//       $scope.clouds = searchResult.data.data;
//     };
//     $scope.searchService = searchServiceFactory('rest/clouds/search', true, $scope, 50);
//
//     $scope.search = function() {
//       $scope.searchService.search();
//     };
//
//     // first load
//     $scope.search();
//
//     // Get environment type list (cached value)
//     $scope.envTypeList = applicationVersionservices.environmentTypeList({}, {}, function(successResponse) {});
//
//   }
// ];
