// archive list is the entry point for browsing and managing csar archives in a4c
define(function (require) {
  'use strict';

  var states = require('states');

  // register archive promotion state
  //TODO should be done in workspace plugin
  states.state('catalog.archives-promotion', {
    url: '/archives-promotion',
    templateUrl: 'views/_ref/catalog/archives/archives_promotion.html',
    // controller: 'ArchivePromotionCtrl',
    menu: {
      id: 'catalog.archives-promotion',
      state: 'catalog.archives-promotion',
      key: 'NAVCATALOG.ARCHIVES_PROMOTION',
      // icon: 'fa fa-archive',
      priority: 20,
    }
  });
  states.forward('catalog', 'catalog.components');
});
