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

  states.state('applications.detail.environment', {
    url: '/environment/:environmentId',
    templateUrl: 'views/_ref/applications/applications_detail_environment.html',
    controller: 'ApplicationEnvironmentCtrl',
    resolve: {
      environment: ['applicationEnvironmentsManager', '$stateParams',
        function(applicationEnvironmentsManager, $stateParams) {
          return _.catch(function() {
            return applicationEnvironmentsManager.get($stateParams.environmentId);
          });
        }
      ]
    },
    params: {
      // optional id of the environment to automatically select when triggering this state
      environmentId: null
    }
  });

  states.forward('applications.detail.environment', 'applications.detail.environment.deploynext');

  modules.get('a4c-applications').controller('ApplicationEnvironmentCtrl',
    ['$scope', '$state', 'application', 'environment', 'menu',
    function ($scope, $state, applicationResponse, environment, menu) {
      $scope.application = applicationResponse.data;
      $scope.environment = environment;
      $scope.statusCss = alienUtils.getStatusCss;

      $scope.menu = menu;

      $scope.onApplication = function($event) {
        $event.preventDefault();
        $event.stopPropagation();
        $state.go('applications.detail', { id: $scope.application.id });
      };
    }
  ]);
});
