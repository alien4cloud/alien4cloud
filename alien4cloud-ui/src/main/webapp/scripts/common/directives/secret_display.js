define(function (require) {
  'use strict';

  var modules = require('modules');

  require('angular-xeditable');
  require('scripts/common/controllers/secret_display');

  modules.get('a4c-common').directive('secretDisplay', function() {
    return {
      templateUrl: 'views/common/secret_display.html',
      controller: 'SecretDisplayCtrl',
      restrict: 'E',
      scope: {
        'translated': '=',
        'definition': '=',
        'editable': '=?',
        'propertyType': '=?',
        'propertyName': '=',
        'propertyValue': '=',
        'capabilityName': '=?',
        'selectedNodeTemplate': '=',
        'onSave': '&'
      },
      link: {}
    };
  });
}); // define
