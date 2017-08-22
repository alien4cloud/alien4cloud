define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

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

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentCtrl',
    ['$scope',
    function ($scope) {

    }
  ]);
});
