define(function (require) {
  'use strict';

  var states = require('states');

  var regsterDeploymentHistoryStates = require('scripts/_ref/applications/services/deployment_detail_register_service');

  states.state('admin.orchestrators.details.deployments', {
    url: '/deployments',
    template: '<ui-view/>',
    onEnter: ['breadcrumbsService','$translate', function(breadcrumbsService, $translate){
      breadcrumbsService.putConfig({
        state: 'admin.orchestrators.details.deployments',
        text: function() {
          return $translate.instant('ORCHESTRATORS.NAV.DEPLOYMENTS');
        }
      });
    }],
    menu: {
      id: 'menu.orchestrators.deployments',
      state: 'admin.orchestrators.details.deployments',
      key: 'ORCHESTRATORS.NAV.DEPLOYMENTS',
      icon: 'fa fa-rocket',
      priority: 200
    }
  });

  regsterDeploymentHistoryStates('admin.orchestrators.details.deployments',function($stateParams) {
    return {orchestratorId: $stateParams.id,};});

}); // define
