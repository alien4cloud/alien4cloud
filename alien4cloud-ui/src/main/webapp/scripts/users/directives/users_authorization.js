define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/users/controllers/users_authorization_directive_ctrl');

  modules.get('a4c-security').directive('alienUserAuthorization', function() {
    return {
      templateUrl: 'views/users/users_authorization_directive.html',
      restrict: 'E',
      scope: {
        'orchestrator': '=',
        'location': '=',
        'crudSupport': '=',
        'notList': '=',
        'onSelectAppRoleCallback': '&',
        'displayAll': '=',
        'displayEmail': '=',
        'displayRoles': '='
      },
      link: function postLink(scope, element, attrs) {
        if (!attrs.checkAppRoleSelectedCallback) {
          scope.checkAppRoleSelectedCallback = null;
        }
        if (!attrs.checkEnvRoleSelectedCallback) {
          scope.checkEnvRoleSelectedCallback = null;
        }
      }
    };
  });
});
