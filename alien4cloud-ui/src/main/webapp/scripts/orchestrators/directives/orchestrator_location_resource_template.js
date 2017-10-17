define(function(require) {
  'use strict';

  var modules = require('modules');

  require('scripts/orchestrators/directives/orchestrator_location_resource_template_ctrl');

  modules.get('a4c-orchestrators').directive('orchestratorLocationResourceTemplate', function() {
    return {
      templateUrl: 'views/orchestrators/directives/orchestrator_location_resource_template.html',
      controller: 'OrchestratorLocationResourceTemplateCtrl',
      restrict: 'E',
      scope: {
        'resourceTemplate': '=',
        'resourceType': '=',
        'resourceCapabilityTypes': '=',
        'resourceDataTypes': '=',
        'dependencies': '=',
        'isPropertyEditable': '&',
        'onDelete': '&',
        'onUpdate': '&',
        'onPropertyUpdate': '&',
        'onCapabilityPropertyUpdate': '&',
        'onPortabilityPropertyUpdate': '&'
      },
      link: {}
    };
  });
});
