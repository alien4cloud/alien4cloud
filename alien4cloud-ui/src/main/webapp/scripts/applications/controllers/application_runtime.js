define(function(require) {
  'use strict';

  var states = require('states');

  states.state('applications.detail.runtime', {
    url: '/runtime',
    template: '<ui-view></ui-view>',
    menu: {
      id: 'am.applications.detail.runtime',
      state: 'applications.detail.runtime',
      key: 'NAVAPPLICATIONS.MENU_RUNTIME',
      icon: 'fa fa-cogs',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer
      priority: 400
    }
  });
});
