// components is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');

  // load other controllers to manage components
  require('scripts/_ref/catalog/controllers/components/policies_list');

  // register root component management state
  states.state('catalog.policies', {
    url: '/policies',
    template: '<ui-view/>',
    controller:'PoliciesCtrl',
    menu: {
      id: 'catalog.policies',
      state: 'catalog.policies',
      key: 'NAVCATALOG.BROWSE_POLICIES',
      priority: 60
    }
  });
  states.forward('catalog.policies', 'catalog.policies.list');

  modules.get('a4c-catalog', ['ui.router']).controller('PoliciesCtrl', ['$scope', 'breadcrumbsService', '$translate',
    function ($scope, breadcrumbsService, $translate) {

      breadcrumbsService.putConfig({
        state : 'catalog.policies',
        text: function(){
          return $translate.instant('NAVCATALOG.BROWSE_POLICIES');
        }
      });
    }
  ]);
});
