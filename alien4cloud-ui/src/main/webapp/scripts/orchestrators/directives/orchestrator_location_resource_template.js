define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators').directive('orchestratorLocationResourceTemplate', function() {
    return {
      templateUrl: 'views/orchestrators/orchestrator_location_resource_template.html',
      restrict: 'E',
      scope: {
        'resourceTemplate': '=',
        'resourceType': '=',
        'resourceCapabilityTypes': '=',
        'dependencies': '=',
        'isEditable': '=',
        'isPropertyEditable': '&',
//        'isPropertyEditableFn': '&',
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
