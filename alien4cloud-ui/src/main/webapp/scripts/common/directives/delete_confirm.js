define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/common/directives/confirm_popup');

  modules.get('a4c-common', ['ui.bootstrap', 'pascalprecht.translate']).directive('deleteConfirm', function() {
    return {
      restrict: 'E',
      templateUrl: 'views/common/confirm_button.html',
      scope: {
        position: '@',
        func: '&',
        fastyle: '@',
        bssize: '@',
        text: '@',
        disable: '='
      }
    };
  });
});
