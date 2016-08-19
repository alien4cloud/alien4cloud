// topology templates list is the entry point for browsing and managing global topologies in a4c
define(function (require) {
  'use strict';

  var states = require('states');

  states.state('topologytemplates.detail.topology.editor', {
    url: '/editor/:version',
    templateUrl: 'views/topology/topology_editor.html',
    controller: 'TopologyCtrl',
    resolve: {
      defaultFilters: [function(){return {};}],
      badges: [function(){return[];}]
    },
    menu: {
      id: 'am.topologytemplate.detail.topology.editor',
      state: 'topologytemplates.detail.topology.editor',
      key: 'NAVAPPLICATIONS.MENU_TOPOLOGY',
      icon: 'fa fa-sitemap',
      show: true,
      priority: 199
    }
  });
  states.forward('topologytemplates.detail.topology', 'topologytemplates.detail.topology.editor');
}); // define
