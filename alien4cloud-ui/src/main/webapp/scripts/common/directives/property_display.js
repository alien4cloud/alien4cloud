'use strict';

angular.module('alienUiApp').directive('propertyDisplay', function() {

  return {
    templateUrl: 'views/fragments/property_display.html',
    restrict: 'E',
    scope: {
      'definition': '=',
      'propertyValue': '=',
      'onSave': '&',
      'editable': '=',
      'condensed': '='
    },
    link: {}
  };

});
