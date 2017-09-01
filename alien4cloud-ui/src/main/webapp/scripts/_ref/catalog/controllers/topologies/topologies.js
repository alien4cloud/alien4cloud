// components is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var states = require('states');

  // load other controllers to manage components
  require('scripts/_ref/catalog/controllers/topologies/topologies_list');

  // register root component management state
  states.state('catalog.topologies', {
    url: '/topologies',
    template: '<ui-view/>',
    menu: {
      id: 'catalog.topologies',
      state: 'catalog.topologies',
      key: 'NAVCATALOG.BROWSE_TOPOLOGIES',
      priority: 40
    }
  });
  states.forward('catalog.topologies', 'catalog.topologies.list');
});
