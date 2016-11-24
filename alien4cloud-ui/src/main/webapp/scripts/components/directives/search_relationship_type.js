define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/components/controllers/component_search');

  modules.get('a4c-components').directive('alienSearchRelationshipType', [function () {
    return {
      templateUrl: 'views/components/search_relationship_type_template.html',
      restrict: 'E',
      controller: 'alienSearchComponentCtrl',
      scope: {
        'refresh': '=',
        'hiddenFilters': '=',
        'onSelectItem': '&',
        'defaultFilters': '=',
        'staticFacets': '='
      },
      link: {
        pre: function (scope) {
          scope.queryComponentType = 'RELATIONSHIP_TYPE';
        }
      }
    };
  }]);
});
