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
      priority: 600,
      options: '{reload: true}'
    }
  });

  var NewApplicationEnvironmentCtrl = ['$scope', '$uibModalInstance', 'inputCandidates',
    function($scope, $uibModalInstance, inputCandidates) {
      $scope.inputCandidates = inputCandidates;
      $scope.newEnvironment = { applicationId: $scope.application.id };
      $scope.create = function(valid) {
        if (valid) {
          if (_.defined($scope.newEnvironment.inputCandidate)) {
            var inputCandidate = $scope.newEnvironment.inputCandidate;
            delete $scope.newEnvironment.inputCandidate;
            $scope.newEnvironment.inputCandidate = inputCandidate.id;
          }
          $uibModalInstance.close($scope.newEnvironment);
        }
      };
      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];

  var SelectEnvironmentToCopyInputCtrl = ['$scope', '$uibModalInstance', 'inputCandidates',
    function($scope, $uibModalInstance, inputCandidates) {
      $scope.inputCandidates = inputCandidates;
      $scope.ok = function(inputCandidate) {
        $uibModalInstance.close(inputCandidate);
      };
      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];

  modules.get('a4c-applications').controller('ApplicationEnvironmentsCtrl',
    ['$scope', '$state', '$translate', 'toaster', 'authService', '$uibModal', 'applicationEnvironmentServices', 'applicationVersionServices', 'appEnvironments', 'archiveVersions',
    function($scope, $state, $translate, toaster, authService, $uibModal, applicationEnvironmentServices, applicationVersionServices, appEnvironments, archiveVersions) {
      $scope.archiveVersions = archiveVersions.data;
      $scope.isManager = authService.hasRole('APPLICATIONS_MANAGER');
      $scope.envTypeList = applicationEnvironmentServices.environmentTypeList({}, {}, function() {});

      $scope.environments = appEnvironments.environments;

      function initEnvironment(environment) {
        // initialize the selectedAppVersion and selectedAppTopoVersion variables based on the environment currentVersionName (basically the application topo version currently configured).
        environment.selectedAppVersion = _.find($scope.versions, function(version) {
          return _.defined(version.topologyVersions[environment.currentVersionName]);
        });
        environment.selectedAppTopoVersion = environment.currentVersionName;
      }
      // Application versions search
      var searchVersions = function() {
        var searchRequestObject = {
          'query': '',
          'from': 0,
          'size': 100
        };
        applicationVersionServices.searchVersion({
          delegateId: $state.params.id
        }, angular.toJson(searchRequestObject), function versionSearchResult(result) {
          $scope.versions = result.data.data;

          _.each($scope.environments, function(environment) {
            initEnvironment(environment);
          });
        });
      };
      searchVersions();

      // Modal to create an new application environment
      $scope.openNewAppEnv = function() {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/applications/application_environment_new.html',
          controller: NewApplicationEnvironmentCtrl,
          scope: $scope,
          resolve: {
            inputCandidates: ['applicationEnvironmentServices', function(applicationEnvironmentServices) {
              return applicationEnvironmentServices.getInputCandidates({
                applicationId: $scope.application.id
              }, angular.toJson({})).$promise.then(function(result) {
                return result.data;
              });
            }]
          }
        });
        modalInstance.result.then(function(environment) {
          applicationEnvironmentServices.create({
            applicationId: $scope.application.id
          }, angular.toJson(environment), function(successResponse) {
            // query the matching dto object
            applicationEnvironmentServices.get({
              applicationId: environment.applicationId,
              applicationEnvironmentId: successResponse.data
            }, function (result) {
              if (_.undefined(result.error) ) {
                initEnvironment(result.data);
                appEnvironments.addEnvironment(result.data);
              }
            });
          });
        });
      };

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
          });
        }
      };

      $scope.forceUserToChangeTopoVersion = function(environment) {
        if (environment.currentVersionName !== environment.selectedAppVersion.version) {
          delete environment.selectedAppTopoVersion;
        }
      };

      function doUpdateTopologyVersion(environment, selectedTopologyVersion, inputCandidate) {
        var inputCandidateId = null;
        if(_.defined(inputCandidate)) {
          inputCandidateId = inputCandidate.id;
        }
        applicationEnvironmentServices.updateTopologyVersion({
          applicationId: $scope.application.id,
          applicationEnvironmentId: environment.id
        }, angular.toJson({
          newTopologyVersion: selectedTopologyVersion,
          environmentToCopyInput: inputCandidateId
        }), function() {
          environment.currentVersionName = selectedTopologyVersion;
          appEnvironments.updateEnvironment(environment);
        });
      }

      $scope.setAppTopologyVersion = function(environment, selectedTopologyVersion) {
        applicationEnvironmentServices.getInputCandidates({
          applicationId: $scope.application.id
        }, angular.toJson({
          applicationEnvironmentId: environment.id,
          applicationTopologyVersion: selectedTopologyVersion
        })).$promise.then(function(result) {
          if (_.defined(result.data) && result.data.length > 0) {
            var modalInstance = $uibModal.open({
              templateUrl: 'views/applications/select_environment_to_copy_inputs.html',
              controller: SelectEnvironmentToCopyInputCtrl,
              resolve: {
                inputCandidates: function(){
                  return result.data;
                }
              }
            });
            modalInstance.result.then(function(inputCandidate) {
              doUpdateTopologyVersion(environment, selectedTopologyVersion, inputCandidate);
            });
          } else {
            doUpdateTopologyVersion(environment, selectedTopologyVersion, null);
          }
        });
      };
      function updateEnvironment(environmentId, fieldName, fieldValue) {
        // update the environments
        var done = false;
        for (var i=0; i < $scope.environments.length && !done; i++) {
          var environment = $scope.environments[i];
          if (environment.id === environmentId) {
            if (fieldName === 'currentVersionId') {
              fieldName = 'currentVersionName';
            }
            environment[fieldName] = fieldValue;
            appEnvironments.updateEnvironment(environment);
            done = true;
          }
        }
      }

      $scope.updateApplicationEnvironment = function(fieldName, fieldValue, environmentId, oldValue) {
        if (_.undefined(oldValue) || fieldValue !== oldValue) {
          var updateApplicationEnvironmentRequest = {};

          var realFieldValue = fieldValue;
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
