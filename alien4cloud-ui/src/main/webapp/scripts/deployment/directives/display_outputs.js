'use strict';

angular.module('alienUiApp').directive('displayOutputs', function() {
  return {
    restrict: 'E',
    templateUrl: 'views/fragments/display_outputs.html',
    // inherites scope from the parent
    scope: true
  };
});
