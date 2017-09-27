define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');
  var _ = require('lodash');
  var alienUtils = require('scripts/utils/alien_utils');

  require('scripts/_ref/applications/controllers/applications_detail_environment_deploynext');
  require('scripts/_ref/applications/controllers/applications_detail_environment_deploycurrent');
  require('scripts/_ref/applications/controllers/applications_detail_environment_history');

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
    }
  });

  // TODO Manually forward to the state with visibility (users should go to summary)
  states.forward('applications.detail.environment', 'applications.detail.environment.deploynext');

  modules.get('a4c-applications').controller('ApplicationEnvironmentCtrl',
    ['$scope', '$state', 'userContextServices', 'application', 'environment', 'menu', 'applicationEnvironmentsManager', 'breadcrumbsService',
      function ($scope, $state, userContextServices, applicationResponse, environment, menu, applicationEnvironmentsManager, breadcrumbsService) {
        $scope.application = applicationResponse.data;
        $scope.environment = environment;
        $scope.statusIconCss = alienUtils.getStatusIconCss;
        $scope.statusTextCss = alienUtils.getStatusTextCss;
        $scope.menu = menu;

        breadcrumbsService.putConfig({
          state: 'applications.detail.environment',
          text: function () {
            return $scope.environment.name;
          },
          onClick: function () {
            $state.go('applications.detail.environment', {
              id: $scope.application.id,
              environmentId: $scope.environment.id
            });
          }
        });

        applicationEnvironmentsManager.onEnvironmentStateChangedCallback = function(env) {
          $scope.setEnvironment(env);
          $scope.$digest();
        };

        function updateMenu() {
          // update menu entry
          var deploycurrent = _.find($scope.menu, { 'state': 'applications.detail.environment.deploycurrent' });
          deploycurrent.disabled = $scope.isState('UNDEPLOYED');
        }

        $scope.isState = function (stateName) {
          return $scope.environment.status === stateName;
        };

        $scope.isAnyState = function (stateNames) {
          return _.indexOf(stateNames, $scope.environment.status) !== -1;
        };

        $scope.setState = function(state){
          $scope.environment.status = state;
          updateMenu();
        };

        $scope.setEnvironment = function (env) {
          $scope.environment = env;
          updateMenu();
        };

        $scope.reloadEnvironment = function() {
          applicationEnvironmentsManager.reload($scope.environment.id, function(environment) {
            $scope.environment = environment;
          });
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

        $scope.$watch('environment', function (env) {
          $scope.setEnvironment(env);
        });

        // update variables related to env status
        $scope.setEnvironment($scope.environment);
      }
    ]);
});
