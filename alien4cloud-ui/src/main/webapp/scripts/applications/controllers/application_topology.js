define(function(require) {
  'use strict';

  var states = require('states');

  states.state('applications.detail.topology', {
    url: '/topology',
    template: '<ui-view></ui-view>',
    resolve: {
      context: function() { return { topologyId: undefined }; },
      preselectedVersion: function() { return undefined; }
    },
    menu: {
      id: 'am.applications.detail.topology',
      state: 'applications.detail.topology',
      key: 'NAVAPPLICATIONS.MENU_TOPOLOGY',
      icon: 'fa fa-sitemap',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEVOPS'], // is deployer
      priority: 200
    }
  });
});
