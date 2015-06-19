define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('angular-ui-select');

  states.state('applications.detail.topology', {
    url: '/topology',
    templateUrl: 'views/topology/topology_editor.html',
    controller: 'TopologyCtrl',
    resolve: {
      topologyId: function() {
        return null;
      }
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

  modules.get('a4c-applications', ['ui.select']);
}); // define
