/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('ApplicationUsersCtrl', ['$scope', 'alienAuthService',
  'applicationServices', 'userServices', 'groupServices', 'application', 'applicationRoles',
  function($scope, alienAuthService, applicationServices, userServices, groupServices, applicationResult, applicationRolesResult) {
    $scope.application = applicationResult.data;
    $scope.appRoles = applicationRolesResult.data;
    $scope.isManager = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    $scope.isDeployer = alienAuthService.hasResourceRole($scope.application, 'DEPLOYMENT_MANAGER');
    $scope.isDevops = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
    $scope.isUser = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_USER');

    // get users related to the application
    $scope.relatedUsers = {};
    if ($scope.application.userRoles) {
      var usernames = [];
      for (var username in $scope.application.userRoles) {
        if ($scope.application.userRoles.hasOwnProperty(username)) {
          usernames.push(username);
        }
      }
      if (usernames.length > 0) {
        userServices.get([], angular.toJson(usernames), function(usersResults) {
          var data = usersResults.data;
          for (var i = 0; i < data.length; i++) {
            $scope.relatedUsers[data[i].username] = data[i];
          }
        });
      }
    }

    $scope.relatedGroups = {};
    if ($scope.application.groupRoles) {
      var groupIds = [];
      for (var groupId in $scope.application.groupRoles) {
        if ($scope.application.groupRoles.hasOwnProperty(groupId)) {
          groupIds.push(groupId);
        }
      }
      if (groupIds.length > 0) {
        groupServices.getMultiple([], angular.toJson(groupIds), function(groupsResults) {
          var data = groupsResults.data;
          for (var i = 0; i < data.length; i++) {
            $scope.relatedGroups[data[i].id] = data[i];
          }
        });
      }
    }

    /**
     * FOR USER SEARCH AND ADD APPLICATION'S ROLE
     */
    var updateRoles = function(roles, role, operation) {
      switch (operation) {
        case 'add':
          if (!roles) {
            roles = [];
          }
          roles.push(role);
          return roles;
        case 'remove':
          var index = roles.indexOf(role);
          roles.splice(index, 1);
          return roles;

        default:
          break;
      }
    };

    $scope.handleRoleSelectionForUser = function(user, role) {
      if (UTILS.isUndefinedOrNull($scope.application.userRoles)) {
        $scope.application.userRoles = {};
      }
      var appUserRoles = $scope.application.userRoles[user.username];

      if (!appUserRoles || appUserRoles.indexOf(role) < 0) {

        applicationServices.userRoles.addUserRole([], {
          applicationId: $scope.application.id,
          username: user.username,
          role: role
        }, function() {
          $scope.application.userRoles[user.username] = updateRoles(appUserRoles, role, 'add');
          if (!$scope.relatedUsers[user.username]) {
            $scope.relatedUsers[user.username] = user;
          }
        });

      } else {
        applicationServices.userRoles.removeUserRole([], {
          applicationId: $scope.application.id,
          username: user.username,
          role: role
        }, function() {
          $scope.application.userRoles[user.username] = updateRoles(appUserRoles, role, 'remove');
        });
      }
    };

    $scope.handleRoleSelectionForGroup = function(group, role) {
      if (UTILS.isUndefinedOrNull($scope.application.groupRoles)) {
        $scope.application.groupRoles = {};
      }
      var appGroupRoles = $scope.application.groupRoles[group.id];

      if (!appGroupRoles || appGroupRoles.indexOf(role) < 0) {
        applicationServices.groupRoles.addGroupRole([], {
          applicationId: $scope.application.id,
          groupId: group.id,
          role: role
        }, function() {
          $scope.application.groupRoles[group.id] = updateRoles(appGroupRoles, role, 'add');
          if (!$scope.relatedGroups[group.id]) {
            $scope.relatedGroups[group.id] = group;
          }
        });

      } else {
        applicationServices.groupRoles.removeGroupRole([], {
          applicationId: $scope.application.id,
          groupId: group.id,
          role: role
        }, function() {
          $scope.application.groupRoles[group.id] = updateRoles(appGroupRoles, role, 'remove');
        });
      }
    };

    $scope.checkAppRoleSelectedForUser = function(user, role) {
      if ($scope.application && $scope.application.userRoles && $scope.application.userRoles[user.username]) {
        return $scope.application.userRoles[user.username].indexOf(role) > -1;
      }
      return false;
    };

    $scope.checkAppRoleSelectedForGroup = function(group, role) {
      if ($scope.application && $scope.application.groupRoles && $scope.application.groupRoles[group.id]) {
        return $scope.application.groupRoles[group.id].indexOf(role) > -1;
      }
      return false;
    };
  }
]);
