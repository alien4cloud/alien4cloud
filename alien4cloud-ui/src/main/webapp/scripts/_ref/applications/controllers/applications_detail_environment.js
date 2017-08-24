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
        function(applicationEnvironmentsManager, $stateParams, userContextServices) {
          return _.catch(function() {
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
    ['$scope', '$state', 'userContextServices', 'application', 'environment', 'menu',
    function ($scope, $state, userContextServices, applicationResponse, environment, menu) {
      $scope.application = applicationResponse.data;
      $scope.environment = environment;
      $scope.statusCss = alienUtils.getStatusCss;

      $scope.menu = menu;

      $scope.goToApplication = function($event) {
        $event.preventDefault();
        $event.stopPropagation();
        userContextServices.clear($scope.application.id);
        $state.go('applications.detail', { id: $scope.application.id });
      };
    }
  ]);
});
