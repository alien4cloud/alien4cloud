define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/users/services/user_services');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');


  modules.get('a4c-security', ['a4c-search']).controller('UsersAuthorizationDirectiveCtrl', ['$scope', '$uibModal',
    function ($scope, $uibModal) {

      // do nothin if there is no resource
      if(_.undefined($scope.resource)){
        return;
      }

      var refreshAuthorizedUsers = function (response) {
        $scope.authorizedUsers = response.data;
      };

      $scope.searchAuthorizedUsers = function () {
        $scope.service.get({}, refreshAuthorizedUsers);
      };
      $scope.searchAuthorizedUsers();

      $scope.openNewUserAuthorizationModal = function () {
        var modalInstance = $uibModal.open({
          templateUrl: _.get($scope, 'authorizeModalTemplateUrl', 'views/users/users_authorization_popup.html'),
          controller: 'UsersAuthorizationModalCtrl',
          resolve:{
            searchConfig:  $scope.buildSearchConfig(),
            authorizedUsers: function() { return $scope.authorizedUsers; }
          },
          scope: $scope
        });

        modalInstance.result.then(function (result) {
          var params = {};
          var force = _.get(result, 'force');
          if(_.defined(force)){
            params.force = force;
          }
          $scope.service.save(params, _.map(result.users, function (user) {
            return user.username;
          }), refreshAuthorizedUsers);
        });
      };

      $scope.revoke = function (user) {
        $scope.service.delete({
          username: user.username
        }, refreshAuthorizedUsers);
      };

      $scope.$watch('resource.id', function(newValue, oldValue){
        if(newValue === oldValue){
          return;
        }
        $scope.searchAuthorizedUsers();
      });
    }
  ]);
});
