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
        // if the properyValus is undefined, the default one will be displayed
        // the onInitWithDefault method allows you to update the propertyValue
        // to the default one
        'onInitWithDefault': '&',
        'onDelete': '&',
        'editable': '=',
        'condensed': '=',
        'deletable': '=?'
      },
      link: {}
    };
  });
}); // define
