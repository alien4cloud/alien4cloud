define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/users/controllers/groups_authorization_directive_ctrl');

  modules.get('a4c-security').directive('alienGroupAuthorization', function () {
    return {
      templateUrl: 'views/users/groups_authorization_directive.html',
      restrict: 'E',
      scope: {
        'resource': '=',
        'service': '='
      }
    };
  });
});
