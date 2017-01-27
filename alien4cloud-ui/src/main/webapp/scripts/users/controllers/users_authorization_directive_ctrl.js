define(function (require) {
  'use strict';
  
  var modules = require('modules');
  var _ = require('lodash');
  
  require('scripts/users/services/user_services');
  require('scripts/orchestrators/services/location_security_service');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');
  
  var NewUserAuthorizationController = ['$scope', '$uibModalInstance', 'searchServiceFactory',
    function ($scope, $uibModalInstance, searchServiceFactory) {
      $scope.batchSize = 5;
      $scope.selectedUsers = [];
      $scope.query = '';
      
      var indexOf = function (selectedUsers, user) {
        return _.findIndex(selectedUsers, function (selectedUser) {
          return selectedUser.username === user.username;
        });
      };
      
      $scope.onSearchCompleted = function (searchResult) {
        $scope.usersData = searchResult.data;
        $scope.selectedUsersInCurrentPage = _.filter($scope.usersData.data, function (searchedUser) {
          return indexOf($scope.selectedUsers, searchedUser) >= 0;
        });
      };
      $scope.searchService = searchServiceFactory('rest/latest/users/search', false, $scope, $scope.batchSize);
      $scope.searchService.search();
      
      $scope.ok = function () {
        if ($scope.selectedUsers.length > 0) {
          $uibModalInstance.close($scope.selectedUsers);
        }
      };
      
      $scope.search = function (event) {
        $scope.selectedUsers = [];
        $scope.searchService.search();
        event.preventDefault();
      };
      
      $scope.toggleSelection = function (user) {
        var indexOfUserInSelected = $scope.selectedUsersInCurrentPage.indexOf(user);
        if (indexOfUserInSelected < 0) {
          $scope.selectedUsersInCurrentPage.push(user);
          $scope.selectedUsers.push(user);
        } else {
          $scope.selectedUsersInCurrentPage.splice(indexOfUserInSelected, 1);
          _.remove($scope.selectedUsers, function (selectedUser) {
            return selectedUser.username === user.username;
          });
        }
      };
      
      $scope.isSelected = function (user) {
        return $scope.selectedUsersInCurrentPage.indexOf(user) >= 0;
      };
      
      $scope.toggleSelectAll = function () {
        // Remove anyway all the elements of the current page from the selected list
        $scope.selectedUsers = _.filter($scope.selectedUsers, function (selectedUser) {
          return indexOf($scope.usersData.data, selectedUser) < 0;
        });
        if ($scope.selectedUsersInCurrentPage.length === $scope.usersData.data.length) {
          $scope.selectedUsersInCurrentPage = [];
        } else {
          $scope.selectedUsersInCurrentPage = $scope.usersData.data.slice();
          $scope.selectedUsers = _.concat($scope.selectedUsers, $scope.selectedUsersInCurrentPage);
        }
      };
      
      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];
  
  modules.get('a4c-security', ['a4c-search']).controller('UsersAuthorizationDirectiveCtrl', ['$scope', '$uibModal', 'locationSecurityService',
    function ($scope, $uibModal, locationSecurityService) {
      var refreshAuthorizedUsers = function (response) {
        $scope.authorizedUsers = response.data;
      };
      
      $scope.searchAuthorizedUsers = function () {
        locationSecurityService.users.get({
          orchestratorId: $scope.orchestrator.id,
          locationId: $scope.location.id
        }, refreshAuthorizedUsers);
      };
      $scope.searchAuthorizedUsers();
      
      $scope.openNewUserAuthorizationModal = function () {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/users/users_authorization_popup.html',
          controller: NewUserAuthorizationController
        });
        
        modalInstance.result.then(function (users) {
          locationSecurityService.users.save({
            orchestratorId: $scope.orchestrator.id,
            locationId: $scope.location.id
          }, _.map(users, function (user) {
            return user.username;
          }), refreshAuthorizedUsers);
        });
      };
      
      $scope.revoke = function (user) {
        locationSecurityService.users.delete({
          orchestratorId: $scope.orchestrator.id,
          locationId: $scope.location.id,
          username: user.username
        }, refreshAuthorizedUsers);
      };
    }
  ]);
});
