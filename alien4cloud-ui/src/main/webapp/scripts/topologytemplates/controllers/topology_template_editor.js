// topology templates list is the entry point for browsing and managing global topologies in a4c
define(function (require) {
  'use strict';

  var states = require('states');

  states.state('topologytemplates.detail.topology.editor', {
    url: '/editor',
    templateUrl: 'views/topology/topology_editor.html',
    controller: 'TopologyCtrl',
    menu: {
      id: 'am.topologytemplate.detail.topology.editor',
      state: 'topologytemplates.detail.topology.editor',
      key: 'EDITOR.MENU_TOPOLOGY',
      icon: 'fa fa-sitemap',
      show: true,
      priority: 1
    }
  });

  states.state('topologytemplates.detail.topology.files', {
    url: '/files',
    templateUrl: 'views/topology/topology_browser.html',
    controller: 'TopologyBrowserCtrl',
    menu: {
      id: 'am.topologytemplate.detail.topology.files',
      state: 'topologytemplates.detail.topology.files',
      key: 'EDITOR.MENU_FILES',
      icon: 'fa fa-folder-open',
      show: true,
      priority: 10
    }
  });

  states.state('topologytemplates.detail.topology.validation', {
    url: '/validation',
    templateUrl: 'views/topology/topology_validation.html',
    controller: 'TopologyValidationCtrl',
    menu: {
      id: 'am.topologytemplate.detail.topology.validation',
      state: 'topologytemplates.detail.topology.validation',
      key: 'EDITOR.MENU_VALIDATION',
      icon: 'fa fa-check',
      show: true,
      priority: 20
    }
  });

  states.state('topologytemplates.detail.topology.history', {
    url: '/history',
    templateUrl: 'views/topology/topology_browser.html',
    controller: 'TopologyHistoryCtrl',
    menu: {
      id: 'am.topologytemplate.detail.topology.history',
      state: 'topologytemplates.detail.topology.history',
      key: 'EDITOR.MENU_HISTORY',
      icon: 'fa fa-history',
      show: true,
      priority: 30
    }
  });

  states.forward('topologytemplates.detail.topology', 'topologytemplates.detail.topology.editor');
}); // define
