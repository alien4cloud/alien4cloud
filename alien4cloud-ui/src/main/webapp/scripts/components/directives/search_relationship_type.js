define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/components/controllers/component_search');

  modules.get('a4c-components').directive('alienSearchRelationshipType', ['$interval', function($interval) {
    return {
      templateUrl: 'views/components/search_relationship_type_template.html',
      restrict: 'E',
      scope: {
        'refresh': '=',
        'hiddenFilters': '=',
        'onSelectItem': '&'
      },
      link: function postLink(scope) {
        scope.queryComponentType = 'RELATIONSHIP_TYPE';
      }
    };
  }]);
});
