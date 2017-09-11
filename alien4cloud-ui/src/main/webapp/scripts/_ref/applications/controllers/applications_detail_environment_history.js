define(function (require) {
  'use strict';

  var states = require('states');

  require('scripts/_ref/applications/controllers/applications_detail_environment_history_list');

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
      icon: '',
      priority: 300
    }
  });

  states.forward('applications.detail.environment.history','applications.detail.environment.history.list');
});
