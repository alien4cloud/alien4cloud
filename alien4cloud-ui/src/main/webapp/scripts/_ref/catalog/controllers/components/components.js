// components is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var states = require('states');

  // load other controllers to manage components
  require('scripts/_ref/catalog/controllers/components/components_list');

  // register root component management state
  states.state('catalog.components', {
    url: '/components',
    template: '<ui-view/>',
    menu: {
      id: 'catalog.components',
      state: 'catalog.components',
      key: 'NAVCATALOG.BROWSE_COMPONENTS',
      priority: 30
    }
  });
  states.forward('catalog.components', 'catalog.components.list');
});
