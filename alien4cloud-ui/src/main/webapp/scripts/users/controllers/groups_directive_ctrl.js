define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/users/services/user_services');
  require('scripts/users/services/group_services');
  require('scripts/common/directives/pagination');

  var NewGroupCtrl = ['$scope', '$uibModalInstance', 'userServices',
    function($scope, $uibModalInstance, userServices) {
      $scope.group = {};
      $scope.alienRoles = userServices.getAlienRoles();
      $scope.create = function(valid) {
        if (valid) {
          $uibModalInstance.close($scope.group);
        }
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };

      $scope.handleGroupRoleSelection = function(group, role) {
        if (_.undefined(group.roles)) {
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

  modules.get('a4c-security', ['a4c-search']).controller('GroupsDirectiveCtrl', ['$scope', '$rootScope', '$uibModal', 'groupServices', 'searchServiceFactory',
    function($scope, $rootScope, $uibModal, groupServices, searchServiceFactory) {

      $scope.query = '';
      $scope.ALL_USERS_GROUP = groupServices.ALL_USERS_GROUP;
      $scope.onSearchCompleted = function(searchResult) {
        $scope.groupsData = searchResult.data;
      };
      $scope.searchService = searchServiceFactory('rest/latest/groups/search', false, $scope, 20);
      $scope.searchService.search();

      /** handle Modal form for group creation */
      $scope.openNewGroupModal = function() {
        var modalInstance = $uibModal.open({
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
      $scope.checkIfAppRoleSelected = function(group, role) {
        if ($scope.checkAppRoleSelectedCallback) {
          return $scope.checkAppRoleSelectedCallback({
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

      $scope.checkIfEnvRoleSelected = function(group, role) {
        if ($scope.checkEnvRoleSelectedCallback) {
          return $scope.checkEnvRoleSelectedCallback({
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

      $scope.$watch('managedAppRoleList', function(newVal) {
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
});
