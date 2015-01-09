/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('ApplicationUsersCtrl', ['$scope', 'alienAuthService',
  'applicationServices', 'userServices', 'groupServices', 'application', 'applicationRoles', 'environmentRoles', 'applicationEnvironmentServices', 'environments',
  function($scope, alienAuthService, applicationServices, userServices, groupServices, applicationResult, applicationRolesResult, environmentRolesResult, applicationEnvironmentServices, environments) {

    $scope.application = applicationResult.data;
    $scope.appRoles = applicationRolesResult.data;
    $scope.environmentRoles = environmentRolesResult.data;
    $scope.isManager = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    $scope.isDeployer = alienAuthService.hasResourceRole($scope.application, 'DEPLOYMENT_MANAGER');
    $scope.isDevops = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
    $scope.isUser = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_USER');

    // get users related to the application
    $scope.relatedUsers = {};
    var loadUsers = function loadUsersToDisplay() {
      var usernames = [];
      // get usernames from application userRoles
      if ($scope.application.userRoles) {
        for (var username in $scope.application.userRoles) {
          if ($scope.application.userRoles.hasOwnProperty(username)) {
            usernames.push(username);
          }
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
    };
    loadUsers();

    // Handle groups in groupRoles from application / environment
    $scope.relatedGroups = {};
    var loadGroups = function loadGroupToDisplay() {
      var groupIds = [];
      // get group ids from application group roles
      if ($scope.application.groupRoles) {
        for (var groupId in $scope.application.groupRoles) {
          if ($scope.application.groupRoles.hasOwnProperty(groupId)) {
            groupIds.push(groupId);
          }
        }
      }
      // get group ids from environment group roles
      if ($scope.selectedEnvironment.groupRoles) {
        for (var groupId in $scope.selectedEnvironment.groupRoles) {
          if ($scope.selectedEnvironment.groupRoles.hasOwnProperty(groupId)) {
            groupIds.push(groupId);
          }
        }
      }
      // get the goot name from group id
      if (groupIds.length > 0) {
        groupServices.getMultiple([], angular.toJson(groupIds), function(groupsResults) {
          var data = groupsResults.data;
          for (var i = 0; i < data.length; i++) {
            $scope.relatedGroups[data[i].id] = data[i];
          }
        });
      }
    };
    loadGroups();

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

    // Handle selection for USER
    $scope.handleAppRoleSelectionForUser = function(user, role) {
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

    $scope.handleEnvRoleSelectionForUser = function(user, role) {
      if (UTILS.isUndefinedOrNull($scope.selectedEnvironment.userRoles)) {
        $scope.selectedEnvironment.userRoles = {};
      }
      var envUserRoles = $scope.selectedEnvironment.userRoles[user.username];
      var envId = $scope.selectedEnvironment.id;
      if (!envUserRoles || envUserRoles.indexOf(role) < 0) {

        applicationEnvironmentServices.userRoles.addUserRole([], {
          applicationEnvironmentId: envId,
          applicationId: $scope.application.id,
          username: user.username,
          role: role
        }, function() {
          $scope.selectedEnvironment.userRoles[user.username] = updateRoles(envUserRoles, role, 'add');
          if (!$scope.relatedUsers[user.username]) {
            $scope.relatedUsers[user.username] = user;
          }
        });

      } else {
        applicationEnvironmentServices.userRoles.removeUserRole([], {
          applicationEnvironmentId: envId,
          applicationId: $scope.application.id,
          username: user.username,
          role: role
        }, function() {
          $scope.selectedEnvironment.userRoles[user.username] = updateRoles(envUserRoles, role, 'remove');
        });
      }
    };

    // Handle selection for GROUP
    $scope.handleAppRoleSelectionForGroup = function(group, role) {
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

    $scope.handleEnvRoleSelectionForGroup = function(group, role) {
      if (UTILS.isUndefinedOrNull($scope.selectedEnvironment.groupRoles)) {
        $scope.selectedEnvironment.groupRoles = {};
      }
      var envGroupRoles = $scope.selectedEnvironment.groupRoles[group.id];
      var envId = $scope.selectedEnvironment.id;
      if (!envGroupRoles || envGroupRoles.indexOf(role) < 0) {

        applicationEnvironmentServices.groupRoles.addGroupRole([], {
          applicationEnvironmentId: envId,
          applicationId: $scope.application.id,
          groupId: group.id,
          role: role
        }, function() {
          $scope.selectedEnvironment.groupRoles[group.id] = updateRoles(envGroupRoles, role, 'add');
          if (!$scope.relatedGroups[group.id]) {
            $scope.relatedGroups[group.id] = group;
          }
        });

      } else {
        applicationEnvironmentServices.groupRoles.removeGroupRole([], {
          applicationEnvironmentId: envId,
          applicationId: $scope.application.id,
          groupId: group.id,
          role: role
        }, function() {
          $scope.selectedEnvironment.groupRoles[group.id] = updateRoles(envGroupRoles, role, 'remove');
        });
      }
    };

    // Checks to update roles on applications or environments
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

    $scope.checkEnvRoleSelectedForUser = function(user, role) {
      if ($scope.selectedEnvironment && $scope.selectedEnvironment.userRoles && $scope.selectedEnvironment.userRoles[user.username]) {
        return $scope.selectedEnvironment.userRoles[user.username].indexOf(role) > -1;
      }
      return false;
    };

    $scope.checkEnvRoleSelectedForGroup = function(group, role) {
      if ($scope.selectedEnvironment && $scope.selectedEnvironment.groupRoles && $scope.selectedEnvironment.groupRoles[group.id]) {
        return $scope.selectedEnvironment.groupRoles[group.id].indexOf(role) > -1;
      }
      return false;
    };

  }
]);
