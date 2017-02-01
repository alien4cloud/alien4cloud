define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/users/services/group_services');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');

  var NewGroupAuthorizationController = ['$scope', '$uibModalInstance', 'searchServiceFactory',
    function ($scope, $uibModalInstance, searchServiceFactory) {
      $scope.batchSize = 5;
      $scope.selectedGroups = [];
      $scope.query = '';

      var indexOf = function (selectedGroups, group) {
        return _.findIndex(selectedGroups, function (selectedGroup) {
          return selectedGroup.id === group.id;
        });
      };

      $scope.onSearchCompleted = function (searchResult) {
        $scope.groupsData = searchResult.data;
        $scope.selectedGroupsInCurrentPage = _.filter($scope.groupsData.data, function (searchedGroup) {
          return indexOf($scope.selectedGroups, searchedGroup) >= 0;
        });
      };
      $scope.searchService = searchServiceFactory('rest/latest/groups/search', false, $scope, $scope.batchSize);
      $scope.searchService.search();

      $scope.ok = function () {
        if ($scope.selectedGroups.length > 0) {
          $uibModalInstance.close($scope.selectedGroups);
        }
      };

      $scope.search = function (event) {
        $scope.selectedGroups = [];
        $scope.searchService.search();
        event.preventDefault();
      };

      $scope.toggleSelection = function (group) {
        var indexOfGroupInSelected = $scope.selectedGroupsInCurrentPage.indexOf(group);
        if (indexOfGroupInSelected < 0) {
          $scope.selectedGroupsInCurrentPage.push(group);
          $scope.selectedGroups.push(group);
        } else {
          $scope.selectedGroupsInCurrentPage.splice(indexOfGroupInSelected, 1);
          _.remove($scope.selectedGroups, function (selectedGroup) {
            return selectedGroup.id === group.id;
          });
        }
      };

      $scope.isSelected = function (group) {
        return $scope.selectedGroupsInCurrentPage.indexOf(group) >= 0;
      };

      $scope.toggleSelectAll = function () {
        // Remove anyway all the elements of the current page from the selected list
        $scope.selectedGroups = _.filter($scope.selectedGroups, function (selectedGroup) {
          return indexOf($scope.groupsData.data, selectedGroup) < 0;
        });
        if ($scope.selectedGroupsInCurrentPage.length === $scope.groupsData.data.length) {
          $scope.selectedGroupsInCurrentPage = [];
        } else {
          $scope.selectedGroupsInCurrentPage = $scope.groupsData.data.slice();
          $scope.selectedGroups = _.concat($scope.selectedGroups, $scope.selectedGroupsInCurrentPage);
        }
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];

  modules.get('a4c-security', ['a4c-search']).controller('GroupsAuthorizationDirectiveCtrl', ['$scope', '$uibModal',
    function ($scope, $uibModal) {
      var refreshAuthorizedGroups = function (response) {
        $scope.authorizedGroups = response.data;
      };
      $scope.searchAuthorizedGroups = function () {
        $scope.service.get({}, refreshAuthorizedGroups);
      };
      $scope.searchAuthorizedGroups();

      $scope.openNewGroupAuthorizationModal = function () {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/users/groups_authorization_popup.html',
          controller: NewGroupAuthorizationController
        });

        modalInstance.result.then(function (groups) {
          $scope.service.save({}, _.map(groups, function (group) {
            return group.id;
          }), refreshAuthorizedGroups);
        });
      };

      $scope.revoke = function (group) {
        $scope.service.delete({
          groupId: group.id
        }, refreshAuthorizedGroups);
      };
    }
  ]);
});
