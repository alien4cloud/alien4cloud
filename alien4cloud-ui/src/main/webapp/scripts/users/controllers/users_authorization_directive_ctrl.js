define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/users/services/user_services');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');

  var NewUserAuthorizationController = ['$scope', '$uibModalInstance', 'searchServiceFactory', 'searchConfig', 'authorizedUsers',
    function ($scope, $uibModalInstance, searchServiceFactory, searchConfig, authorizedUsers) {
      $scope.batchSize = 5;
      $scope.selectedUsers = [];
      $scope.query = '';

      var indexOf = function (selectedUsers, user) {
        return _.findIndex(selectedUsers, {'username': user.username});
      };

      $scope.onSearchCompleted = function (searchResult) {
        $scope.usersData = searchResult.data;
        $scope.selectedUsersInCurrentPage = _.filter($scope.usersData.data, function (searchedUser) {
          return indexOf($scope.selectedUsers, searchedUser) >= 0;
        });
      };
      var url = _.get(searchConfig, 'url', 'rest/latest/users/search');
      var useParams = _.get(searchConfig, 'useParams', false);
      var params = _.get(searchConfig, 'params', null);
      $scope.searchService = searchServiceFactory(url, useParams, $scope, $scope.batchSize, null, null, null, params);
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
          _.remove($scope.selectedUsers, {'username': user.username});
        }
      };

      $scope.isSelected = function (user) {
        return $scope.selectedUsersInCurrentPage.indexOf(user) >= 0;
      };

      $scope.isAuthorized = function(user) {
        return indexOf(authorizedUsers, user) >= 0;
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
          //remove already authorized users
          _.each(authorizedUsers, function(user){
            _.remove($scope.selectedUsersInCurrentPage, {'username': user.username});
          });
          $scope.selectedUsers = _.concat($scope.selectedUsers, $scope.selectedUsersInCurrentPage);
        }

      };

      $scope.getSelectAllClass = function(){
        var allCurrentPageUsernames = _.map($scope.usersData.data, 'username');
        var allSelected = _.map(_.union(authorizedUsers, $scope.selectedUsersInCurrentPage), 'username');
        var allSelectedInCurrentPage = _.intersection(allSelected, allCurrentPageUsernames);

        if(_.isEmpty(allSelectedInCurrentPage)){
          return 'fa-square-o';
        } else if (_.every(allCurrentPageUsernames, function(username){
          return _.includes(allSelectedInCurrentPage, username);
        })) {
          return 'fa-check-square-o';
        }else {
          return 'fa-minus-square-o';
        }
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];

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
          controller: NewUserAuthorizationController,
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
