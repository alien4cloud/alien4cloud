define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common').directive('osIcon', function() {
    return {
      templateUrl : 'views/common/os_icon.html',
      restrict : 'E',
      scope : {
        'osType' : '='
      }
    };
  });
}); // define
