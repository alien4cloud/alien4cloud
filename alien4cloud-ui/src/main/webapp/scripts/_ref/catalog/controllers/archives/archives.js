// archive list is the entry point for browsing and managing csar archives in a4c
define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');

  require('scripts/_ref/catalog/controllers/archives/archives_list');
  require('scripts/_ref/catalog/controllers/archives/archives_detail');

  // register archives management state
  states.state('catalog.archives', {
    url: '/archives',
    template: '<ui-view/>',
    controller: 'ArchivesCtrl',
    menu: {
      id: 'catalog.archives',
      state: 'catalog.archives',
      key: 'NAVCATALOG.MANAGE_ARCHIVES',
      priority: 10,
    }
  });
  states.forward('catalog.archives', 'catalog.archives.list');

  modules.get('a4c-catalog', ['ui.router']).controller('ArchivesCtrl', ['breadcrumbsService', '$translate',
    function (breadcrumbsService, $translate) {
      breadcrumbsService.putConfig({
        state : 'catalog.archives',
        text: function(){
          return $translate.instant('NAVBAR.MENU_CSARS');
        }
      });
    }
  ]);
});
