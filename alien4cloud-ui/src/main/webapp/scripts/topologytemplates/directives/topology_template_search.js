define(function (require) {
  'use strict';

  require('scripts/common/directives/facets');

  var modules = require('modules');

  require('scripts/topologytemplates/controllers/topology_template_search');

  modules.get('a4c-topology-templates', []).directive('topologyTemplateSearch', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/topologytemplates/topology_template_search.html',
      controller: 'TopologyTemplateSearchCtrl',
      scope: {
        onSelect: '&',
        onSelectForClone: '&',
        onSearchConfig: '&',
        archiveLink: '@',
        defaultFilters: '=',
        staticFacets: '='
      }
    };
  });
});
