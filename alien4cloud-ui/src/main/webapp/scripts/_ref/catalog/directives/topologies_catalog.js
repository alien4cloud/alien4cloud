define(function (require) {
  'use strict';

  require('scripts/common/directives/facets');

  var modules = require('modules');

  require('scripts/_ref/catalog/controllers/topologies/topologies_search');

  modules.get('a4c-catalog', []).directive('a4cTopologiesCatalog', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/_ref/catalog/topologies/topologies_catalog.html',
      controller: 'a4cSearchTopologyCtrl',
      scope: {
        'onSelectItem': '&',
        'globalContext': '=',
        'defaultFilters': '=',
        'staticFacets': '=',
        'isSelected': '&?'
      }
    };
  });
});
