define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_info');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_runtimeeditor');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_workflow');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_executions');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_tasks');
  require('scripts/log/directives/log_search_panel');
  require('scripts/log/directives/log_search_display');
  require('scripts/log/controllers/log');
  require('scripts/log/application_deployment_log_states');
  require('scripts/applications/services/application_event_services');
  require('scripts/applications/services/runtime_event_service');
  require('scripts/_ref/applications/services/secret_display_modal');

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
    ['$scope', 'menu', 'deploymentServices', 'topologyJsonProcessor', 'applicationServices', '$uibModal', 'a4cRuntimeEventService', '$state', 'toaster', '$timeout', 'secretDisplayModal',
      function ($scope, menu, deploymentServices, topologyJsonProcessor, applicationServices, $uibModal, a4cRuntimeEventService, $state, toaster, $timeout, secretDisplayModal) {
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

        $scope.doUndeploy = function() {
          secretDisplayModal($scope.secretProviderConfigurations).then(function (secretProviderInfo) {
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
          if(event.rawType === 'paasmessagemonitorevent' || event.rawType === 'paasworkflowmonitorevent') {
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
