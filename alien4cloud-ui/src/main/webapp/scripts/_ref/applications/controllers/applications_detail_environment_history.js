define(function (require) {
  'use strict';

  var states = require('states');
  var regsterDeploymentHistoryStates = require('scripts/_ref/applications/services/deployment_detail_register_service');

  states.state('applications.detail.environment.history', {
    url: '/history',
    template: '<ui-view/>',
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
      priority: 300
    }
  });

  regsterDeploymentHistoryStates('applications.detail.environment.history',function($stateParams) {
    return {environmentId: $stateParams.environmentId};});
});
