define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/common/directives/pagination');

  states.state('applications.detail.environments', {
    url: '/environment',
    templateUrl: 'views/applications/application_environments.html',
    controller: 'ApplicationEnvironmentsCtrl',
    menu: {
      id: 'am.applications.detail.environments',
      state: 'applications.detail.environments',
      key: 'NAVAPPLICATIONS.MENU_ENVIRONMENT',
      icon: 'fa fa-share-alt',
      roles: ['APPLICATION_MANAGER'],
      priority: 600
    }
  });

  var NewApplicationEnvironmentCtrl = ['$scope', '$modalInstance', '$resource', '$state',
    function($scope, $modalInstance, $resource, $state) {
      $scope.environment = {};
      $scope.create = function(valid, envType, version) {
        if (valid) {
          // prepare the good request
          var applicationId = $state.params.id;
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

  modules.get('a4c-applications').controller('ApplicationEnvironmentsCtrl',
    ['$scope', '$state', '$translate', 'toaster', 'authService', '$modal', 'applicationEnvironmentServices', 'applicationVersionServices', 'appEnvironments', 'archiveVersions',
    function($scope, $state, $translate, toaster, authService, $modal, applicationEnvironmentServices, applicationVersionServices, appEnvironments, archiveVersions) {
      $scope.archiveVersions = archiveVersions.data;
      $scope.isManager = authService.hasRole('APPLICATIONS_MANAGER');
      $scope.envTypeList = applicationEnvironmentServices.environmentTypeList({}, {}, function() {});

      // Application versions search
      var searchVersions = function() {
        var searchRequestObject = {
          'query': '',
          'from': 0,
          'size': 50
        };
        applicationVersionServices.searchVersion({
          delegateId: $state.params.id
        }, angular.toJson(searchRequestObject), function versionSearchResult(result) {
          $scope.versions = result.data.data;
        });

      };
      searchVersions();

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
            $scope.search().then(function(searchResult){
              var environments = searchResult.data.data;
              var pushed = false;
              for(var i=0; i < environments.length && !pushed; i++) {
                if(environments[i].id === successResponse.data) {
                  appEnvironments.addEnvironment(environments[i]);
                  pushed = true;
                }
              }
            });
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
            if(result.data) {
              appEnvironments.removeEnvironment(appEnvId);
            }
            $scope.search();
          });
        }
      };

      $scope.getVersionByName = function(name) {
        return _.find($scope.versions, {'version': name});
      };

      var getVersionIdByName = function(name) {
        return _.result($scope.getVersionByName(name), 'id');
      };

      function updateEnvironment(environmentId, fieldName, fieldValue) {
        // update the environments
        var done = false;
        for(var i=0; i < $scope.searchAppEnvResult.length && !done; i++) {
          var environment = $scope.searchAppEnvResult[i];
          if(environment.id === environmentId) {
            environment[fieldName] = fieldValue;
            appEnvironments.updateEnvironment(environment);
            done = true;
          }
        }
      }

      $scope.updateApplicationEnvironment = function(fieldName, fieldValue, environmentId, oldValue) {
        if (fieldName !== 'name' || fieldValue !== oldValue) {
          var updateApplicationEnvironmentRequest = {};

          var realFieldValue = fieldValue;
          if (fieldName === 'currentVersionId') {
            realFieldValue = getVersionIdByName(fieldValue);
          }
          updateApplicationEnvironmentRequest[fieldName] = realFieldValue;

          return applicationEnvironmentServices.update({
            applicationId: $scope.application.id,
            applicationEnvironmentId: environmentId
          }, angular.toJson(updateApplicationEnvironmentRequest)).$promise.then(function(response) {
            if (_.defined(response.error)) {
              toaster.pop('error', $translate.instant('ERRORS.' + response.error.code), response.error.message, 4000, 'trustedHtml', null);
            } else {
              updateEnvironment(environmentId, fieldName, realFieldValue);
            }
          }, function(errorResponse) {
            return $translate.instant('ERRORS.' + errorResponse.data.error.code);
          });
        }
      };
    }
  ]);
});
