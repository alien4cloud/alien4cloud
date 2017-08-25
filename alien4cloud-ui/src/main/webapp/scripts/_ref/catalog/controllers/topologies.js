// archive list is the entry point for browsing and managing csar archives in a4c
define(function (require) {
  'use strict';

  var states = require('states');

  // register archives management state
  states.state('catalog.topologies', {
    url: '/topologies',
    template: '<h2> View in progress...</h2>',
    // controller: 'ArchiveListCtrl',
    menu: {
      id: 'catalog.topologies',
      state: 'catalog.topologies',
      key: 'NAVCATALOG.BROWSE_TOPOLOGIES',
      // icon: 'fa fa-archive',
      priority: 40,
    }
  });
  states.forward('catalog', 'catalog.components');
});
