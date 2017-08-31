define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_info');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_runtimeeditor');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_workflow');

  require('scripts/applications/services/application_event_services');
  require('scripts/applications/services/runtime_event_service');
    
  states.state('applications.detail.environment.deploycurrent', {
    url: '/deploy_current',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent.html',
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
    ['$scope', 'menu', 'deploymentServices', 'topologyJsonProcessor', 'applicationServices', '$uibModal', 'a4cRuntimeEventService', '$state',
      function ($scope, menu, deploymentServices, topologyJsonProcessor, applicationServices, $uibModal, a4cRuntimeEventService, $state) {
        $scope.menu = menu;

        // register for deployment events.
        var pageStateId = $state.current.name;
        a4cRuntimeEventService($scope, $scope.application.id, pageStateId);

        //////////////////////////////////////
        ///  CONFIRMATION BEFORE UNDEPLOYMENT
        ///
        var UndeployConfirmationModalCtrl = ['$scope', '$uibModalInstance', 'applicationName', 'topologyDTO', 'environment',
          function ($scope, $uibModalInstance, applicationName, topologyDTO, environment) {
            $scope.deployedVersion = topologyDTO.topology.archiveVersion;
            $scope.locationResources = {};
            _.each(_.keys(topologyDTO.topology.substitutedNodes), function (name) {
              $scope.locationResources[name] = topologyDTO.topology.nodeTemplates[name];
            });
            $scope.application = applicationName;
            $scope.environment = environment;

            $scope.undeploy = function () {
              $uibModalInstance.close();
            };
            $scope.close = function () {
              $uibModalInstance.dismiss();
            };
          }
        ];

        function doUndeploy() {
          $scope.setState('INIT_DEPLOYMENT');
          applicationServices.deployment.undeploy({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.environment.id
          }, function () {
            $scope.environment.status = 'UNDEPLOYMENT_IN_PROGRESS';
            $scope.setEnvironment($scope.environment);
            $scope.stopEvent();
          }, function () {
            $scope.reloadEnvironment();
          });
        }

        $scope.undeploy = function () {
          var modalInstance = $uibModal.open({
            templateUrl: 'views/applications/undeploy_confirm_modal.html',
            controller: UndeployConfirmationModalCtrl,
            resolve: {
              applicationName: function () {
                return $scope.application.name;
              },
              topologyDTO: function () {
                return $scope.topology;
              },
              environment: function () {
                return $scope.environment;
              }
            },
            size: 'lg'
          });

          modalInstance.result.then(function () {
            doUndeploy();
          });
        };

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
          });
        }

        loadTopologyRuntime();
      }
    ]);
});
