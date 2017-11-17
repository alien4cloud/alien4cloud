define(function (require) {
  'use strict';

  var modules = require('modules');

  require('angular-xeditable');
  require('scripts/common/controllers/secret_display');

  modules.get('a4c-common').directive('secretDisplay', function() {
    return {
      templateUrl: 'views/common/secret_display.html',
      restrict: 'E',
      scope: {
        'translated': '=',
        'definition': '=',
        'propertyType': '=?',
        'propertyName': '=',
        'propertyValue': '='
      },
      link: {}
    };
  });
}); // define
