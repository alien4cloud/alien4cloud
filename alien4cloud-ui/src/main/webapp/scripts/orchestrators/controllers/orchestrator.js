define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/common/services/alien_resource');
  require('scripts/orchestrators/controllers/orchestrator_new');
  require('scripts/orchestrators/services/orchestrator_service');
  require('scripts/orchestrators/controllers/orchestrator_details');

  states.state('admin.orchestrators', {
    url: '/orchestrators',
    template: '<ui-view/>',
    controller: 'OrchestratorsCtrl',
    menu: {
      id: 'am.admin.orchestrators',
      state: 'admin.orchestrators',
      key: 'NAVADMIN.MENU_ORCHESTRATORS',
      icon: 'fa fa-magic',
      priority: 301
    }
  });

  states.forward('admin.orchestrators', 'admin.orchestrators.list');

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap', 'a4c-common']).controller('OrchestratorsCtrl',
    ['$state', 'breadcrumbsService', '$translate',
      function ($state, breadcrumbsService, $translate) {
        breadcrumbsService.putConfig({
          state: 'admin.orchestrators',
          text: function () {
            return $translate.instant('NAVADMIN.MENU_ORCHESTRATORS');
          }
        });
      }
    ]);
}); // define
