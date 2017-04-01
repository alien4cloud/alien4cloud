define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-deployment').directive('displayInputs', function() {
    return {
      restrict: 'E',
      templateUrl: 'views/deployment/display_inputs.html',
      scope: {
        inputProperties: '=',
        orchestratorProperties: '=',
        collapsable: '=',
        classes: '='
      }
    };
  });
}); // define
