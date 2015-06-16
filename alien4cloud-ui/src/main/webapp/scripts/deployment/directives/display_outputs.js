define(function (require) {
  'use strict';

  var modules = require('modules');

  angular.module('alienUiApp').directive('displayOutputs', function() {
    return {
      restrict: 'E',
      templateUrl: 'views/deployment/display_outputs.html',
      // inherites scope from the parent
      scope: true
    };
  });
}); // define
