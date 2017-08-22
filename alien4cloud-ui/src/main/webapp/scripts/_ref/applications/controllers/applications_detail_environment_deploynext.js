define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('applications.detail.environment.deploynext', {
    url: '/deploy_next',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext.html',
    controller: 'ApplicationEnvDeployNextCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext',
      state: 'applications.detail.environment.deploynext',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT',
      icon: '',
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployNextCtrl',
    ['$scope',
    function ($scope) {

    }
  ]);
});
