define(function (require) {
  'use strict';

  var modules = require('modules');

  require('scripts/components/controllers/component_search');

  modules.get('a4c-components').directive('a4cComponentCatalog', function () {
    return {
      templateUrl: 'views/_ref/catalog/components/components_catalog.html',
      restrict: 'E',
      controller: 'alienSearchComponentCtrl',
      scope: {
        'refresh': '=',
        'displayDetailPannel': '=',
        'onSelectItem': '&',
        'globalContext': '=',
        'dragAndDropEnabled': '=',
        'heightInfo': '=',
        'widthInfo': '=',
        'defaultFilters': '=',
        'staticFacets': '=',
        'badges': '='
      },
      link: {
        pre: function (scope) {
          scope.queryComponentType = 'NODE_TYPE';
        }
      }
    };
  });
});
