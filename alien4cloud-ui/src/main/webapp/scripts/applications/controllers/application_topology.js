define(function(require) {
  'use strict';

  var states = require('states');

  states.state('applications.detail.topology', {
    url: '/topology',
    templateUrl: 'views/topology/topology_editor_layout.html',
    controller: 'TopologyEditorCtrl',
    resolve: {
      context: function() { return {}; },
      workspaces: ['application', function(application) {
        var workspaceId = 'app:' + application.data.id;
        return [workspaceId, 'ALIEN_GLOBAL_WORKSPACE'];
      }]
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
