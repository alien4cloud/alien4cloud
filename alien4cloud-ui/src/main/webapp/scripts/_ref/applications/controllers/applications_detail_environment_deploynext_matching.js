define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('applications.detail.environment.deploynext.matching', {
    url: '/matching',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_matching.html',
    controller: 'AppEnvDeployNextMatchingCtrl',
    menu: {
      id: 'applications.detail.environment.deploynext.matching',
      state: 'applications.detail.environment.deploynext.matching',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_NEXT.MATCHING',
      icon: '',
      priority: 500,
      step: {
        taskCodes: ['NO_NODE_MATCHES', 'NODE_NOT_SUBSTITUTED', 'IMPLEMENT', 'REPLACE']
      }
    }
  });

  modules.get('a4c-applications').controller('AppEnvDeployNextMatchingCtrl',
    ['$scope',
    function ($scope) {

    }
  ]);
});
