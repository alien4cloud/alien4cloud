define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  states.state('applications.detail.environment.history.detail.info', {
    url: '/info',
    templateUrl: 'views/_ref/applications/applications_detail_environment_history_detail_info.html',
    controller: 'ApplicationEnvHistoryDetailInfoCtrl',
    menu: {
      id: 'applications.detail.environment.history.detail.info',
      state: 'applications.detail.environment.history.detail.info',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.INFO',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvHistoryDetailInfoCtrl',
    ['$scope', 'deploymentDTO',
      function ($scope, deploymentDTO) {
        $scope.deploymentDTO = deploymentDTO.data;

      }
    ]);
});
