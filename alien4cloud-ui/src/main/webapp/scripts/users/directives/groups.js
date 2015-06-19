define(function (require) {
  'use strict';
  
  var modules = require('modules');
  require('scripts/users/controllers/groups_directive_ctrl');

  modules.get('a4c-security').directive('alienSearchGroup', function() {
    return {
      templateUrl: 'views/users/search_groups_directive.html',
      restrict: 'E',
      scope: {
        'crudSupport': '=',
        'managedAppRoleList': '=',
        'managedEnvRoleList': '=',
        'checkAppRoleSelectedCallback': '&',
        'checkEnvRoleSelectedCallback': '&',
        'onSelectAppRoleCallback': '&',
        'onSelectEnvRoleCallback': '&',
        'onSelectAppGroupCallback': '&',
        'onSelectEnvGroupCallback': '&',
        'displayAll': '=',
        'displayEmail': '=',
        'displayRoles': '=',
        'displayDescription': '='
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
