'use strict';

var NewGroupCtrl = ['$scope', '$modalInstance', 'userServices', 'groupServices',
  function($scope, $modalInstance, userServices) {
    $scope.group = {};
    $scope.alienRoles = userServices.getAlienRoles();
    $scope.create = function(valid) {
      if (valid) {
        $modalInstance.close($scope.group);
      }
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    $scope.handleGroupRoleSelection = function(group, role) {
      if (UTILS.isUndefinedOrNull(group.roles)) {
        group.roles = [];
      }
      var roleIndex = group.roles.indexOf(role);
      var roleNotExist = group.roles.indexOf(role) < 0;
      if (roleNotExist) {
        group.roles.push(role);
      } else {
        group.roles.splice(roleIndex, 1);
      }
    };
  }
];

angular.module('alienUiApp').controller('GroupsDirectiveCtrl', ['$scope', '$rootScope', '$modal', 'groupServices', 'searchServiceFactory',
  function($scope, $rootScope, $modal, groupServices, searchServiceFactory) {

    $scope.query = '';
    $scope.onSearchCompleted = function(searchResult) {
      $scope.groupsData = searchResult.data;
    };
    $scope.searchService = searchServiceFactory('rest/groups/search', false, $scope, 20);
    $scope.searchService.search();

    /** handle Modal form for group creation */
    $scope.openNewGroupModal = function() {
      var modalInstance = $modal.open({
        templateUrl: 'views/users/new_group.html',
        controller: NewGroupCtrl
      });

      modalInstance.result.then(function(newGroup) {
        groupServices.create([], angular.toJson(newGroup), function() {
          $scope.searchService.search();
        });
      });
    };

    //prevent closing when clicking on a role
    $scope.preventClose = function(event) {
      event.stopPropagation();
    };

    //check if a role is selected for a group
    $scope.checkIfRoleSelected = function(group, role) {
      if ($scope.checkRoleSelectedCallback) {
        return $scope.checkRoleSelectedCallback({
          group: group,
          role: role
        });
      } else {
        //default checker
        if (group.roles) {
          return group.roles.indexOf(role) > -1;
        }
      }

      //return false either
      return false;
    };

    $scope.$watch('managedRoleList', function(newVal) {
      if (!newVal) {
        return;
      }
      $scope.searchService.search();
    });

    $scope.remove = function(group) {
      groupServices.remove({
        groupId: group.id
      }, function() {
        $scope.searchService.search();
        $rootScope.$emit('groupsChanged');
      });
    };

    $scope.groupChanged = function(group, fieldName, fieldValue) {
      var updateGroupRequest = {};
      updateGroupRequest[fieldName] = fieldValue;
      groupServices.update({
        groupId: group.id
      }, angular.toJson(updateGroupRequest));
      $rootScope.$emit('groupsChanged');
    };
  }
]);
