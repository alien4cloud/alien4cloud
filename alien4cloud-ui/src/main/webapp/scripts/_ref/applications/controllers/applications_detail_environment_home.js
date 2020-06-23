define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  states.state('applications.detail.environment.home', {
    url: '/history',
    templateUrl: 'views/_ref/applications/applications_detail_environment_home.html',
    controller: 'ApplicationEnvDeployHomeCtrl',
    onEnter: ['breadcrumbsService','$translate', function(breadcrumbsService, $translate){
      breadcrumbsService.putConfig({
        state: 'applications.detail.environment.home',
        text: function () {
          return $translate.instant('NAVAPPLICATIONS.MENU_HOME');
        }
      });
    }],
    menu: {
      id: 'applications.detail.environment.home',
      state: 'applications.detail.environment.home',
      key: 'NAVAPPLICATIONS.MENU_HOME',
      icon: '',
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployHomeCtrl',
      ['$scope', 'menu', 'deploymentServices', 'topologyJsonProcessor', 'applicationServices', '$uibModal', 'a4cRuntimeEventService', '$state', 'toaster', '$timeout', 'secretDisplayModal',
        function ($scope, menu, deploymentServices, topologyJsonProcessor, applicationServices, $uibModal, a4cRuntimeEventService, $state, toaster, $timeout, secretDisplayModal) {
          $scope.menu = menu;

          // this controller may load a small topo DTO (like wizard do)
          // deploy next seems to need sotmehing to be able to start
        }
      ]
  );
});
