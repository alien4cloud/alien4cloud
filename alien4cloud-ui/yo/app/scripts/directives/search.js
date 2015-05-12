'use strict';

angular.module('alienUiApp').directive('alienSearchComponent', ['$interval', function($interval) {
  return {
    templateUrl: 'views/fragments/search_component_template.html',
    restrict: 'E',
    scope: {
      'refresh': '=',
      'displayDetailPannel': '=',
      'onSelectItem': '&',
      'globalContext': '=',
      'dragAndDropEnabled': '=',
      'heightInfo': '=',
      'widthInfo': '='
    },
    link: function(scope, element) {
      scope.queryComponentType = 'NODE_TYPE';

      function resize() {
        var listHeight = scope.heightInfo.height - element.offset().top - 74;
        scope.listHeight = scope.globalContext ? 'height: ' + listHeight + 'px' : '';
      }
      scope.$watch('heightInfo', function() {
        resize();
      });
      scope.$watch('widthInfo', function() {
        resize();
      });

      $interval(resize, 10, 1);
    }
  };
}]);

angular.module('alienUiApp').directive('alienSearchRelationship', function() {
  return {
    templateUrl: 'views/fragments/search_relationship_template.html',
    restrict: 'E',
    scope: {
      'refresh': '=',
      'hiddenFilters': '=',
      'onSelectItem': '&'
    },
    link: function postLink(scope) {
      scope.queryComponentType = 'RELATIONSHIP_TYPE';
    }
  };
});

angular.module('alienUiApp').directive('alienSearchUser', function() {
  return {
    templateUrl: 'views/users/search_users_directive.html',
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

angular.module('alienUiApp').directive('alienSearchGroup', function() {
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
