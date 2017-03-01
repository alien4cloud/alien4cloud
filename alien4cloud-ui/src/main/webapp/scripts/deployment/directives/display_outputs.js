define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-deployment').directive('displayOutputs', function() {
    return {
      restrict: 'E',
      templateUrl: 'views/deployment/display_outputs.html',
      // inherites scope from the parent
      scope: true,
      link: function(scope, element, attrs) {
        scope._ = _;
        scope.collapsable = scope.$eval(attrs.collapsable);
        if(_.defined(scope.deploymentContext)) {
          scope.selectedEnvironment = scope.deploymentContext.selectedEnvironment;
          scope.$watch('deploymentContext.selectedEnvironment', function(newEnv) {
            scope.selectedEnvironment = newEnv;
          }, false);
        }
      }
    };
  });
}); // define
