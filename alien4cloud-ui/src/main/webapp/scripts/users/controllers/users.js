'use strict';

angular.module('alienUiApp').controller('UsersCtrl', ['$scope', '$rootScope', 'userServices', 'groupServices',
  function($scope, $rootScope, userServices, groupServices) {

    $scope.tabSelected = function(tabName) {
      $rootScope.$emit(tabName + 'ViewActive');
    };

    userServices.getAlienRoles().$promise.then(function(rolesResult) {
      $scope.alienRoles = rolesResult.data;
    });

    $scope.handleGroupRoleSelection = function(group, role) {
      if (!group.roles || group.roles.indexOf(role) < 0) {
        groupServices.addRole([], {
          groupId: group.id,
          role: role
        }, function success(result) {
          if (UTILS.isUndefinedOrNull(result.error)) {
            if (UTILS.isUndefinedOrNull(group.roles)) {
              group.roles = [];
            }
            group.roles.push(role);
            $rootScope.$emit('groupsChanged');
          }
        });
      } else {
        groupServices.removeRole([], {
          groupId: group.id,
          role: role
        }, function success(result) {
          if (UTILS.isUndefinedOrNull(result.error)) {
            if (UTILS.isDefinedAndNotNull(group.roles)) {
              var roleIndex = group.roles.indexOf(role);
              if (roleIndex > -1) {
                group.roles.splice(roleIndex, 1);
              }
            }
            $rootScope.$emit('groupsChanged');
          }
        });
      }
    };

    $scope.handleRoleSelection = function(user, role) {
      if (!user.roles || user.roles.indexOf(role) < 0) {
        userServices.addRole([], {
          username: user.username,
          role: role
        }, function() {
          userServices.addToRoleArray(user, role);
          userServices.initRolesToDisplay(user);
        });
      } else {
        userServices.removeRole([], {
          username: user.username,
          role: role
        }, function() {
          userServices.removeFromRoleArray(user, role);
          userServices.initRolesToDisplay(user);
        });
      }
    };

    $scope.handleGroupSelection = function(invalid, user, groupId) {
      if (invalid) {
        return;
      }
      if (UTILS.arrayContains(user.groups, groupId)) {
        groupServices.removeUser([], {
          username: user.username,
          groupId: groupId
        }, function(success) {
          userServices.removeFromGroupArray(user, groupId);
          user.groupRoles = success.data.groupRoles;
          userServices.initRolesToDisplay(user);
        });
      } else {
        groupServices.addUser([], {
          username: user.username,
          groupId: groupId
        }, function(success) {
          userServices.addToGroupArray(user, groupId);
          user.groupRoles = success.data.groupRoles;
          userServices.initRolesToDisplay(user);
        });
      }
    };

    $scope.checkUserRoleSelected = function(user, role) {
      if (user.roles) {
        return user.roles.indexOf(role) > -1;
      }
      return false;
    };
  }
]);
