define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators').directive('orchestratorLocationResources', function() {
    return {
      templateUrl: 'views/orchestrators/orchestrator_location_resources.html',
      restrict: 'E',
      scope: {
        'resourcesTemplates': '=',
        'resourcesTypes': '=',
        'resourcesTypesMap': '=',
        'context': '=?',
        'showCatalog': '=?'
      },
      link: {}
    };
  });
});
