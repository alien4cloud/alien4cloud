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
          templateUrl: 'views/users/users_authorization_popup.html',
          controller: 'UsersAuthorizationModalCtrl',
          resolve:{
            searchConfig:  $scope.buildSearchConfig(),
            authorizedUsers: function() { return $scope.authorizedUsers; }
          }
        });

        modalInstance.result.then(function (users) {
          $scope.service.save({}, _.map(users, function (user) {
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
