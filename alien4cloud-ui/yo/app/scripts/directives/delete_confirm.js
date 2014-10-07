'use strict';

angular.module('alienUiApp').directive('deleteConfirm', function() {
  return {
    restrict: 'E',
    templateUrl: 'views/fragments/confirm_button.html',
    scope: {
      position: '@',
      func: '&',
      fastyle: '@',
      bssize: '@'
    }
  };
});