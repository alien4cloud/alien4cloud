define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
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
