define(function (require) {
  'use strict';

  var modules = require('modules');

  require('scripts/_ref/catalog/controllers/components/components_search');

  modules.get('a4c-catalog').directive('a4cComponentCatalog', function () {
    return {
      templateUrl: 'views/_ref/catalog/components/components_catalog.html',
      restrict: 'E',
      controller: 'a4cSearchComponentCtrl',
      scope: {
        'onSelectItem': '&',
        'globalContext': '=',
        'dragAndDropEnabled': '=',
        'defaultFilters': '=',
        'staticFacets': '=',
        'badges': '=',
        'componentType': '@'
      }
    };
  });
});
