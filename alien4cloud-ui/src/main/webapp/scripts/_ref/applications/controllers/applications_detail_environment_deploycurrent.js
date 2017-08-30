define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_info');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_runtimeeditor');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent_workflow');

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
    ['$scope', 'menu', 'deploymentServices', 'topologyJsonProcessor',
      function ($scope, menu, deploymentServices, topologyJsonProcessor) {
        $scope.menu = menu;

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
