define(function (require) {
  'use strict';
  
  var modules = require('modules');
  require('scripts/users/controllers/apps_authorization_directive_ctrl');
  
  modules.get('a4c-security').directive('alienAppAuthorization', function () {
    return {
      templateUrl: 'views/users/apps_authorization_directive.html',
      restrict: 'E',
      scope: {
        'orchestrator': '=',
        'location': '='
      }
    };
  });
});
