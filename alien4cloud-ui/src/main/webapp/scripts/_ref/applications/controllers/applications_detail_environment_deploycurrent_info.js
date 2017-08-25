define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('applications.detail.environment.deploycurrent.info', {
    url: '/info',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_info.html',
    controller: 'ApplicationEnvDeployCurrentInfoCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.info',
      state: 'applications.detail.environment.deploycurrent.info',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.INFO',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentInfoCtrl',
    ['$scope',
    function ($scope) {

    }
  ]);
});
