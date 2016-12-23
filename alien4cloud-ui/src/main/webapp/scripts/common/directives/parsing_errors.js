define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common', []).directive('parsingErrors', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/common/parsing_errors.html',
      scope: {
        uploadInfo: '='
      }
    };
  });
});
