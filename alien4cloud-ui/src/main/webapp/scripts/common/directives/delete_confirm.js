define(function (require) {
  'use strict';

  var modules = require('modules');
  require('button-confirm');

  modules.get('a4c-common', ['angular-utils-ui']).directive('deleteConfirm', function() {
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
