define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('applications.detail.environment.deploycurrent.logs', {
    url: '/logs',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_logs.html',
    controller: 'ApplicationEnvDeployCurrentLogsCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.logs',
      state: 'applications.detail.environment.deploycurrent.logs',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.LOGS',
      icon: 'fa fa-newspaper-o',
      priority: 300
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentLogsCtrl',
    ['$scope',
    function ($scope) {

    }
  ]);
});
