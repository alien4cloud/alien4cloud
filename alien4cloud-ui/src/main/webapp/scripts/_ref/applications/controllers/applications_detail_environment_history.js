define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');
  var regsterDeploymentHistoryStates = require('scripts/_ref/applications/services/deployment_detail_register_service');

  states.state('applications.detail.environment.history', {
    url: '/history',
    template: '<ui-view/>',
    controller: 'ApplicationEnvDeployHistoryCtrl',
    onEnter: ['breadcrumbsService','$translate', function(breadcrumbsService, $translate){
      breadcrumbsService.putConfig({
        state: 'applications.detail.environment.history',
        text: function () {
          return $translate.instant('NAVAPPLICATIONS.MENU_HISTORY');
        }
      });
    }],
    menu: {
      id: 'applications.detail.environment.history',
      state: 'applications.detail.environment.history',
      key: 'NAVAPPLICATIONS.MENU_HISTORY',
      priority: 400,
      icon: ''
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployHistoryCtrl',
      ['$scope', 'menu', 'deploymentServices', 'topologyJsonProcessor', 'applicationServices', '$uibModal', 'a4cRuntimeEventService', '$state', 'toaster', '$timeout', 'secretDisplayModal',
        function ($scope, menu, deploymentServices, topologyJsonProcessor, applicationServices, $uibModal, a4cRuntimeEventService, $state, toaster, $timeout, secretDisplayModal) {
          $scope.menu = menu;

          // this controller may load a small topo DTO (like wizard do)
          // deploy next seems to need sotmehing to be able to start
        }
      ]
  );

  regsterDeploymentHistoryStates('applications.detail.environment.history',function($stateParams) {
    return { environmentId: $stateParams.environmentId };
  });
});
