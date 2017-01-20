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
      $scope.onSearchCompleted = function (searchResult) {
        $scope.selectedUsers = [];
        $scope.usersData = searchResult.data;
      };
      $scope.searchService = searchServiceFactory('rest/latest/users/search', false, $scope, $scope.batchSize);
      $scope.searchService.search();
      
      $scope.ok = function () {
        if ($scope.selectedUsers.length > 0) {
          $uibModalInstance.close($scope.selectedUsers);
        }
      };
      
      $scope.toggleSelection = function (user) {
        var indexOfUserInSelected = $scope.selectedUsers.indexOf(user);
        if (indexOfUserInSelected < 0) {
          $scope.selectedUsers.push(user);
        } else {
          $scope.selectedUsers.splice(indexOfUserInSelected, 1);
        }
      };
      
      $scope.isSelected = function (user) {
        return $scope.selectedUsers.indexOf(user) >= 0;
      };
      
      $scope.toggleSelectAll = function () {
        if ($scope.selectedUsers.length === $scope.usersData.data.length) {
          $scope.selectedUsers = [];
        } else {
          $scope.selectedUsers = $scope.usersData.data.slice();
        }
      };
      
      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];
  
  modules.get('a4c-security', ['a4c-search']).controller('UsersAuthorizationDirectiveCtrl', ['$scope', '$uibModal', 'locationSecurityService',
    function ($scope, $uibModal, locationSecurityService) {
      $scope.searchAuthorizedUsers = function () {
        locationSecurityService.users.get({
          orchestratorId: $scope.orchestrator.id,
          locationId: $scope.location.id
        }, function (response) {
          $scope.authorizedUsers = response.data;
        });
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
          }), $scope.searchAuthorizedUsers);
        });
      };
      
      $scope.revoke = function (user) {
        locationSecurityService.users.delete({
          orchestratorId: $scope.orchestrator.id,
          locationId: $scope.location.id,
          username: user.username
        }, $scope.searchAuthorizedUsers);
      };
    }
  ]);
});
