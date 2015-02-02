'use strict';

angular.module('alienUiApp').directive('osIcon', function() {
  return {
    templateUrl : 'views/fragments/os_icon.html',
    restrict : 'E',
    scope : {
      'osType' : '='
    }
  };
});
