define(function(require) {
  'use strict';

  var modules = require('modules');

  require('scripts/orchestrators/directives/orchestrator_location_policy_template_ctrl');

  modules.get('a4c-orchestrators').directive('orchestratorLocationPolicyTemplate', function() {
    return {
      templateUrl: 'views/orchestrators/directives/orchestrator_location_policy_template.html',
      controller: 'OrchestratorLocationPolicyTemplateCtrl',
      restrict: 'E',
      scope: {
        'resourceTemplate': '=',
        'resourceType': '=',
        'resourceDataTypes': '=',
        'dependencies': '=',
        'isPropertyEditable': '&',
        'onDelete': '&',
        'onUpdate': '&',
        'onPropertyUpdate': '&',
        'onPortabilityPropertyUpdate': '&'
      },
      link: {}
    };
  });
});
