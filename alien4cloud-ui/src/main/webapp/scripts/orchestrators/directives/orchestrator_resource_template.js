define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-orchestrators').directive('orchestratorResourceTemplate', function() {
    return {
      templateUrl: 'views/orchestrators/orchestrator_resource_template.html',
      restrict: 'E',
      scope: {
        'resourceTemplate': '=',
        'resourceType': '=',
        'context': '=?',
        'onSave': '&',
        'onDelete': '&'
      },
      link: {}
    };
  });
});