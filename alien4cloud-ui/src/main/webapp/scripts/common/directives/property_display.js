define(function (require) {
  'use strict';

  var modules = require('modules');

  require('angular-xeditable');
  require('scripts/common/controllers/property_display');
  require('scripts/common/filters/strings');

  modules.get('a4c-common').directive('propertyDisplay', function() {
    return {
      templateUrl: 'views/common/property_display.html',
      restrict: 'E',
      scope: {
        'translated': '=',
        'definition': '=',
        'propertyType': '=?',
        'propertyName': '=',
        'propertyValue': '=',
        'dependencies': '=?',
        'onSave': '&',
        'onDelete': '&',
        'editable': '=',
        'condensed': '=',
        'deletable': '=?',
        'isSecret': '='
      },
      link: {}
    };
  });
}); // define
