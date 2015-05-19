/* global UTILS */
'use strict';

angular.module('alienUiApp').directive('metaPropertiesDisplay', function () {
  return {
    templateUrl: 'views/fragments/meta_properties_display.html',
    restrict: 'E',
    scope: {
      'properties': '='
    },
    link: {}
  };
});
