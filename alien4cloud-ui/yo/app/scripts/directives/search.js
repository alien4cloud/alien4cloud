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
      'heightInfo': '='
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
      'managedRoleList': '=',
      'checkRoleSelectedCallback': '&',
      'onSelectRoleCallback': '&',
      'onSelectGroupCallback': '&',
      'displayAll': '=',
      'displayEmail': '=',
      'displayRoles': '='
    },
    link: function postLink(scope, element, attrs) {
      if (!attrs.checkRoleSelectedCallback) {
        scope.checkRoleSelectedCallback = null;
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
      'managedRoleList': '=',
      'checkRoleSelectedCallback': '&',
      'onSelectRoleCallback': '&',
      'onSelectGroupCallback': '&',
      'displayAll': '=',
      'displayEmail': '=',
      'displayRoles': '=',
      'displayDescription': '='
    },
    link: function postLink(scope, element, attrs) {
      if (!attrs.checkRoleSelectedCallback) {
        scope.checkRoleSelectedCallback = null;
      }
    }
  };
});
