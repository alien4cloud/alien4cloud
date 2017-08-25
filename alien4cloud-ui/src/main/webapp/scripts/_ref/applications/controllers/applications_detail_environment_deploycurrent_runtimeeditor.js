define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  states.state('applications.detail.environment.deploycurrent.runtimeeditor', {
    url: '/runtime_editor',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_runtimeeditor.html',
    controller: 'ApplicationEnvDeployCurrentRuntimeEditorCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.runtimeeditor',
      state: 'applications.detail.environment.deploycurrent.runtimeeditor',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.RUNTIME_EDITOR',
      icon: 'fa fa-play',
      priority: 200
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentRuntimeEditorCtrl',
    ['$scope',
    function ($scope) {

    }
  ]);
});
