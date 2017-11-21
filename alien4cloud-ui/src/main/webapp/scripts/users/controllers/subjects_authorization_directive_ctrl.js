define(function (require) {
  'use strict';

  /**
  ** controller for simple subject (such as user and group) authorizations directives
  **
  */

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');
  require('scripts/users/directives/authorize_users');
  require('scripts/users/directives/authorize_groups');

  modules.get('a4c-security', ['a4c-search']).controller('SubjectAuthorizationDirectiveCtrl', ['$scope',
    function ($scope) {
      /**
      *$scope.getId == > defined in link function on the directive
      *$scope.getRevokeParams == > defined in link function on the directive
      *
      **/

      // do nothin if there is no resource
      if (_.undefined($scope.resource)) {
        return;
      }
      var refreshAuthorizedSubjects = function (response) {
        $scope.authorizedSubjects = response.data;
      };

      var searchAuthorizedSubjects = function () {
        $scope.service.get({}, refreshAuthorizedSubjects);
      };
      searchAuthorizedSubjects();

      $scope.onModalClose = function(result) {
        $scope.service.save(_.map(result.subjects, $scope.getId), refreshAuthorizedSubjects);
      };

      $scope.revoke = function (subject) {
        $scope.service.delete($scope.getRevokeParams(subject), refreshAuthorizedSubjects);
      };

      $scope.$watch('resource.id', function(newValue, oldValue) {
        if (newValue === oldValue) {
          return;
        }
        searchAuthorizedSubjects();
      });
    }
  ]);
});
