define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('applications.detail.environment.deploynext.deploy', {
    url: '/deploy',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_deploy.html',
    controller: 'AppEnvDeployNextDeployCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.deploy',
      state: 'applications.detail.environment.deploynext.deploy',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT.DEPLOY',
      icon: '',
      priority: 600,
      step: {
        taskCodes: ['NODE_FILTER_INVALID', 'ORCHESTRATOR_PROPERTY', 'PROPERTIES', 'SCALABLE_CAPABILITY_INVALID']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextDeployCtrl',
    ['$scope',
    function ($scope) {

    }
  ]);
});
