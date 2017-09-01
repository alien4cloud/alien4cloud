// components list is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var states = require('states');

  // load other locations to manage catalog contain
  require('scripts/_ref/catalog/controllers/archives/archives');
  require('scripts/_ref/catalog/controllers/components/components');

  require('scripts/_ref/catalog/controllers/topologies/topologies');

  // register catalog root state
  states.state('catalog', {
    url: '/catalog',
    templateUrl: 'views/_ref/layout/tab_menu_layout.html',
    controller: 'LayoutCtrl',
    menu: {
      id: 'menu.catalog',
      state: 'catalog',
      key: 'NAVBAR.MENU_CATALOG',
      icon: 'fa fa-cubes',
      priority: 60,
    }
  });
  states.forward('catalog', 'catalog.components');
});
