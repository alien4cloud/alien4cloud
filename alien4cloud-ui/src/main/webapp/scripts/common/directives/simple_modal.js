define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/common/controllers/simple_modal');

  modules.get('a4c-common').directive('simpleModal', function() {
    return {
      restrict: 'E',
      templateUrl: 'views/common/simple_modal_wrapper.html',
      scope: {
        title: '@',
        content: '@',
        key: '@',
        primary: '='
      }
    };
  });
});
