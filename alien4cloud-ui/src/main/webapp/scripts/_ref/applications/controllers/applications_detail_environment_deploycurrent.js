define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_info');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_runtimeeditor');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_workflow');

  require('scripts/applications/services/application_event_services');
  require('scripts/applications/services/runtime_event_service');
  require('scripts/_ref/applications/controllers/applications_detail_environment_secret_modal');

  states.state('applications.detail.environment.deploycurrent', {
    url: '/deploy_current',
    templateUrl: 'views/_ref/layout/vertical_menu_left_layout.html',
    controller: 'ApplicationEnvDeployCurrentCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent',
      state: 'applications.detail.environment.deploycurrent',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT',
      icon: '',
      priority: 200
    }
  });

  states.forward('applications.detail.environment.deploycurrent', 'applications.detail.environment.deploycurrent.info');

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentCtrl',
    ['$scope', 'menu', 'deploymentServices', 'topologyJsonProcessor', 'applicationServices', '$uibModal', 'a4cRuntimeEventService', '$state', 'toaster', '$timeout', '$q',
      function ($scope, menu, deploymentServices, topologyJsonProcessor, applicationServices, $uibModal, a4cRuntimeEventService, $state, toaster, $timeout, $q) {
        $scope.menu = menu;

        function exitIfUndeployed(){
          if ($scope.environment.status === 'UNDEPLOYED') {
            $state.go('applications.detail.environment.deploynext');
            return true;
          }
        }

        // We should not process any further if the env is undeployed
        if (exitIfUndeployed()) {
          return;
        }

        // Add watcher to switch back to 'deploy next' when undeployed completed
        $scope.$watch('environment', function () {
          exitIfUndeployed();
        }, true);

        // register for deployment events.
        var pageStateId = $state.current.name;
        a4cRuntimeEventService($scope, $scope.application.id, pageStateId);

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

        $scope.doUndeploy = function() {
          $scope.openSecretCredentialModal().then(function (secretProviderInfo) {
            var secretProviderInfoRequest = {};
            if (_.defined(secretProviderInfo)) {
              secretProviderInfoRequest.secretProviderConfiguration = secretProviderInfo;
              secretProviderInfoRequest.credentials = secretProviderInfo.credentials;
            }
            $scope.setState('INIT_DEPLOYMENT');
            applicationServices.deployment.undeploy({
              applicationId: $scope.application.id,
              applicationEnvironmentId: $scope.environment.id
            }, angular.toJson(secretProviderInfoRequest), function () {
              $scope.environment.status = 'UNDEPLOYMENT_IN_PROGRESS';
              $scope.setEnvironment($scope.environment);
              $scope.stopEvent();
            }, function () {
              $scope.reloadEnvironment();
            });
          });
        };

        function refreshInstancesInfos() {
          applicationServices.runtime.get({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.environment.id
          }, function(successResult) {
            if (!_.isEqual($scope.topology.instances, successResult.data)) {
              $scope.topology.instances = successResult.data;
            }
          });
        }

        function loadTopologyRuntime() {
          delete $scope.topology;
          $scope.$broadcast('a4cRuntimeTopologyLoading');
          deploymentServices.runtime.getTopology({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.environment.id
          }, function (successResult) { // get the topology
            $scope.topology = successResult.data;
            topologyJsonProcessor.process($scope.topology);
            // dispatch an event through the scope
            $scope.$broadcast('a4cRuntimeTopologyLoaded');

            //load instances informations. For output updates
            refreshInstancesInfos();
          });
        }

        loadTopologyRuntime();

        $scope.$on('a4cRuntimeEventReceived', function(angularEvent, event) {
          if(event.rawType === 'paasmessagemonitorevent') {
            return;
          }
          // topology has changed
          if (!$scope.isWaitingForRefresh) {
            $scope.isWaitingForRefresh = true;
            $timeout(function() {
              $scope.isWaitingForRefresh = false;
              refreshInstancesInfos();
            }, 1000);
          }
        });
      }
    ]);
});
