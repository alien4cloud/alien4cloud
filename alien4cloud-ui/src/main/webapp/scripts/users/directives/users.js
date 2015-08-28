define(function (require) {
  'use strict';

  var modules = require('modules');
  require('scripts/users/controllers/users_directive_ctrl');

  modules.get('a4c-security').directive('alienSearchUser', function() {
    return {
      templateUrl: 'views/users/search_users_directive.html',
      restrict: 'E',
      scope: {
        'crudSupport': '=',
        'managedAppRoleList': '=',
        'managedEnvRoleList': '=',
        'notEditableRoleList': '=',
        'checkAppRoleSelectedCallback': '&',
        'checkEnvRoleSelectedCallback': '&',
        'onSelectAppRoleCallback': '&',
        'onSelectEnvRoleCallback': '&',
        'onSelectAppGroupCallback': '&',
        'onSelectEnvGroupCallback': '&',
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
