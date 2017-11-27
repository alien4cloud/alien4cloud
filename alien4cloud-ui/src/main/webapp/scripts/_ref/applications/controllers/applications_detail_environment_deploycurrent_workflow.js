define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/topology/controllers/topology_editor_workflows');
  require('scripts/topology/directives/workflow_rendering');
  require('scripts/_ref/applications/controllers/applications_detail_environment_secret_modal');

  states.state('applications.detail.environment.deploycurrent.workflow', {
    url: '/runtime_editor',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_workflow.html',
    controller: 'ApplicationEnvDeployCurrentWorkflowCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.workflow',
      state: 'applications.detail.environment.deploycurrent.workflow',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.WORKFLOW_VIEWER',
      icon: 'fa fa-code-fork fa-rotate-90',
      priority: 300
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentWorkflowCtrl',
    ['$scope', 'topoEditDisplay', 'topoEditWf', 'applicationServices', 'breadcrumbsService', '$translate', '$state', 'toaster', '$uibModal', '$q',
      function ($scope, topoEditDisplay, topoEditWf, applicationServices, breadcrumbsService, $translate, $state, toaster, $uibModal, $q) {

        breadcrumbsService.putConfig({
          state : 'applications.detail.environment.deploycurrent.workflow',
          text: function(){
            return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.WORKFLOW_VIEWER');
          },
          onClick: function(){
            $state.go('applications.detail.environment.deploycurrent.workflow');
          }
        });

        applicationServices.getSecretProviderConfigurationsForCurrentDeployment.get({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.environment.id
        }, undefined, function(success) {
          if (_.defined(success.data)) {
            $scope.secretProviderConfigurations = success.data;
          }
        });

        $scope.openSecretCredentialModal = function () {
          // credentialDescriptor
          var promise;
          switch (_.size($scope.secretProviderConfigurations)) {
            case 1:
              return $uibModal.open({
                templateUrl: 'views/_ref/applications/applications_detail_environment_secret_modal.html',
                controller: 'SecretCredentialsController',
                resolve: {
                  secretCredentialInfos : function() {
                    return $scope.secretProviderConfigurations;
                  }
                }
              }).result;
            case 0:
              promise = $q.defer();
              promise.resolve();
              return promise.promise;
            default:
              toaster.pop('error', 'Multi locations', 'is not yet supported', 0, 'trustedHtml', null);
              promise = $q.defer();
              promise.resolve();
              return promise.promise;
          }
        };

        $scope.displays = {
          workflows: { active: true, size: 400, selector: '#workflow-menu-box', only: ['workflows'] }
        };
        topoEditDisplay($scope, '#workflow-graph');
        topoEditWf($scope);

        $scope.$on('a4cRuntimeTopologyLoaded', function () {
          $scope.workflows.setCurrentWorkflowName('install');
        });
        $scope.workflows.setCurrentWorkflowName('install');

        $scope.launchWorkflow = function () {
          $scope.openSecretCredentialModal().then(function (secretProviderInfo) {
            var secretProviderInfoRequest = {};
            if (_.defined(secretProviderInfo)) {
              secretProviderInfoRequest.secretProviderConfiguration = $scope.secretProviderConfigurations[0];
              secretProviderInfoRequest.credentials = secretProviderInfo.credentials;
            }

            $scope.isLaunchingWorkflow = true;
            applicationServices.launchWorkflow({
              applicationId: $scope.application.id,
              applicationEnvironmentId: $scope.environment.id,
              workflowName: $scope.currentWorkflowName
            }, angular.toJson(secretProviderInfoRequest), function success() {
              $scope.isLaunchingWorkflow = false;
            });

          });
        };


      }]);
});
