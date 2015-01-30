'use strict';

angular.module('alienUiApp').directive('displayOutputAttributes', function() {
  return {
    restrict: 'E',
    templateUrl: 'views/fragments/display_output_attributes.html',
    // inherites scope from the parent
    scope: true
  };
});
