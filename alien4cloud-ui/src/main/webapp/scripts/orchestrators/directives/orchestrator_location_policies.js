define(function(require) {
  'use strict';

  var modules = require('modules');

  require('scripts/orchestrators/directives/orchestrator_location_policies_ctrl');

  modules.get('a4c-orchestrators').directive('orchestratorLocationPolicies', function() {
    return {
      templateUrl: 'views/orchestrators/directives/common_orchestrator_location_resources.html',
      controller: 'OrchestratorLocationPoliciesTemplateCtrl',
      restrict: 'E',
      scope: {
        'resourcesTypesMap': '=',
        'resourcesTemplates': '=',
        // 'providedTypes': '=?',
        'context': '=?',
        // 'showCatalog': '=?',
        'showMultiSelect': '=?',
      },
      link: {}
    };
  });
});
