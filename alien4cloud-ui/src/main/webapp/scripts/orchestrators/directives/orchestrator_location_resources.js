define(function(require) {
  'use strict';

  var modules = require('modules');

  require('scripts/orchestrators/directives/orchestrator_location_resources_ctrl');

  modules.get('a4c-orchestrators').directive('orchestratorLocationResources', function() {
    return {
      templateUrl: 'views/orchestrators/directives/common_orchestrator_location_resources.html',
      restrict: 'E',
      controller: 'OrchestratorLocationResourcesTemplateCtrl',
      scope: {
        'resourcesTypesMap': '=',
        'resourcesTemplates': '=',
        'providedTypes': '=?',
        'context': '=?',
        'showCatalog': '=?',
        'showMultiSelect': '=?'
      },
      link: {}
    };
  });
});
