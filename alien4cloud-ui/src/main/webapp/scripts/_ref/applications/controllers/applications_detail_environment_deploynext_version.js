define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('applications.detail.environment.deploynext.version', {
    url: '/version',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_version.html',
    controller: 'AppEnvDeployNextVersionCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.version',
      state: 'applications.detail.environment.deploynext.version',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT.VERSION',
      icon: '',
      priority: 100,
      step: {
        taskCodes: []
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextVersionCtrl',
    ['$scope',
    function ($scope) {

    }
  ]);
});
