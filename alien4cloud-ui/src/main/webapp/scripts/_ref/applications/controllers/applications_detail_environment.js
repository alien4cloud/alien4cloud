/*
* Application list is the entry point for the application management.
*/
define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');
  var _ = require('lodash');
  var alienUtils = require('scripts/utils/alien_utils');

  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent');

  require('scripts/common/services/user_context_services');

  states.state('applications.detail.environment', {
    url: '/environment/:environmentId',
    templateUrl: 'views/_ref/applications/applications_detail_environment.html',
    controller: 'ApplicationEnvironmentCtrl',
    resolve: {
      environment: ['applicationEnvironmentsManager', '$stateParams', 'userContextServices',
        function (applicationEnvironmentsManager, $stateParams, userContextServices) {
          return _.catch(function () {
            var environment = applicationEnvironmentsManager.get($stateParams.environmentId);
            userContextServices.setEnvironmentId(environment.applicationId, $stateParams.environmentId);
            return environment;
          });
        }
      ]
    },
    params: {
      // optional id of the environment to automatically select when triggering this state
      environmentId: null
    }
  });

  // TODO Manually forward to the state with visibility (users should go to summary)
  states.forward('applications.detail.environment', 'applications.detail.environment.deploynext');

  modules.get('a4c-applications').controller('ApplicationEnvironmentCtrl',
    ['$scope', '$state', 'userContextServices', 'application', 'environment', 'menu', 'topologyJsonProcessor', 'deploymentServices',
      function ($scope, $state, userContextServices, applicationResponse, environment, menu, topologyJsonProcessor, deploymentServices) {
        $scope.application = applicationResponse.data;
        $scope.environment = environment;
        $scope.statusIconCss = alienUtils.getStatusIconCss;
        $scope.statusTextCss = alienUtils.getStatusTextCss;
        $scope.isDeployed = false;

        $scope.menu = menu;

        $scope.setEnvironment = function(env){
          $scope.environment = env;
        };

        $scope.goToApplication = function ($event) {
          $event.preventDefault();
          $event.stopPropagation();
          userContextServices.clear($scope.application.id);
          $state.go('applications.detail', { id: $scope.application.id });
        };

        $scope.onItemClick = function ($event, menuItem) {
          if (menuItem.disabled) {
            $event.preventDefault();
            $event.stopPropagation();
          }
        };

        function updateIsDeployed() {
          $scope.isDeployed = ($scope.environment.status !== 'UNDEPLOYED');
          // update menu entry
          var deploycurrent = _.find($scope.menu, { 'state': 'applications.detail.environment.deploycurrent' });
          deploycurrent.disabled = !$scope.isDeployed;
        }

        $scope.$watch('environment', function () {
          updateIsDeployed();
        });

        updateIsDeployed();
      }
    ]);
});
