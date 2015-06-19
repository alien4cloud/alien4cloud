define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-deployment').directive('displayOutputs', function() {
    return {
      restrict: 'E',
      templateUrl: 'views/deployment/display_outputs.html',
      // inherites scope from the parent
      scope: true
    };
  });
}); // define
