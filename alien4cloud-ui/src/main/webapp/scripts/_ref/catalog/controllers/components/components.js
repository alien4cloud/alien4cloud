// components is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');

  // load other controllers to manage components
  require('scripts/_ref/catalog/controllers/components/components_list');

  // register root component management state
  states.state('catalog.components', {
    url: '/components',
    template: '<ui-view/>',
    controller:'ComponentsCtrl',
    menu: {
      id: 'catalog.components',
      state: 'catalog.components',
      key: 'NAVCATALOG.BROWSE_COMPONENTS',
      priority: 30
    }
  });
  states.forward('catalog.components', 'catalog.components.list');


  modules.get('a4c-catalog', ['ui.router']).controller('ComponentsCtrl', ['breadcrumbsService', '$translate',
    function (breadcrumbsService, $translate) {
      breadcrumbsService.putConfig({
        state : 'catalog.components',
        text: function(){
          return $translate.instant('NAVBAR.MENU_COMPONENTS');
        }
      });
    }
  ]);

});
