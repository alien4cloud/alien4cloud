'use strict';

angular.module('alienUiApp').directive('displayOutputAttributes', function() {
  return {
    restrict: 'E',
    templateUrl: 'views/fragments/display_output_attributes.html',
    scope: {
      applicationId: '@',
      environmentId: '@'
    }
  };
});
