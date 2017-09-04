define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  states.state('applications.detail.users', {
    url: '/users',
    templateUrl: 'views/applications/application_users.html',
    resolve: {
      applicationRoles: ['$resource',
        function($resource) {
          return $resource('rest/latest/auth/roles/application', {}, {
            method: 'GET'
          }).get().$promise;
        }
      ],
      environmentRoles: ['$resource',
        function($resource) {
          return $resource('rest/latest/auth/roles/environment', {}, {
            method: 'GET'
          }).get().$promise;
        }
      ]
    },
    controller: 'ApplicationUsersCtrl',
    menu: {
      id: 'am.applications.detail.users',
      state: 'applications.detail.users',
      key: 'NAVAPPLICATIONS.MENU_USERS',
      icon: 'fa fa-users',
      roles: ['APPLICATION_MANAGER'],
      priority: 10000
    }
  });

  modules.get('a4c-applications').controller('ApplicationUsersCtrl', ['$scope', '$translate', 'breadcrumbsService', 'authService', 'applicationServices', 'userServices', 'groupServices', 'application', 'applicationRoles', 'environmentRoles', 'applicationEnvironmentServices', 'applicationEnvironmentsManager',
    function($scope, $translate, breadcrumbsService, authService, applicationServices, userServices, groupServices, applicationResult, applicationRolesResult, environmentRolesResult, applicationEnvironmentServices, applicationEnvironmentsManager) {
      breadcrumbsService.putConfig({
        state : 'applications.detail.users',
        text: function(){
          return $translate.instant('NAVAPPLICATIONS.MENU_USERS');
        }
      });

      $scope.application = applicationResult.data;
      $scope.selectedEnvironment = applicationEnvironmentsManager.environments[0];
      $scope.environments = applicationEnvironmentsManager.environments;
      $scope.appRoles = applicationRolesResult.data;
      $scope.environmentRoles = environmentRolesResult.data;

      // To ease later code just initialize the user and group roles fields in app and environments.
      function initField(obj, field) {
        if (_.undefined(obj[field])) {
          obj[field] = {};
        }
      }
      initField($scope.application, 'userRoles');
      initField($scope.application, 'groupRoles');

      $scope.usersByRole = {};
      $scope.groupsByRole = {};

      // Initialze the ids of groups and users defined in app or environment roles
      var usernames = [], groupIds = [];
      _.each($scope.application.userRoles, function(userRoles, username) {
        usernames.push(username);
      });
      _.each($scope.application.groupRoles, function(groupRoles, groupId) {
        groupIds.push(groupId);
      });
      _.each($scope.environments, function(environment) {
        initField(environment, 'userRoles');
        initField(environment, 'groupRoles');
        _.each(environment.userRoles, function(userRoles, username) {
          usernames.push(username);
        });
        _.each(environment.groupRoles, function(groupRoles, groupId) {
          groupIds.push(groupId);
        });
      });

      // Maps of users and groups currently fetched for details display.
      var userMap = {}, groupMap = {};

      function updateUserEnvironmentRoles() {
        _.each($scope.environmentRoles, function(envRole) {
          $scope.usersByRole[envRole] = [];
          _.each($scope.selectedEnvironment.userRoles, function(roles, userId) {
            if(roles.indexOf(envRole) >= 0) {
              _.safePush($scope.usersByRole, envRole, userMap[userId]);
            }
          });
        });
      }
      function updateGroupEnvironmentRoles() {
        _.each($scope.environmentRoles, function(envRole) {
          $scope.groupsByRole[envRole] = [];
          _.each($scope.selectedEnvironment.groupRoles, function(roles, groupId) {
            if(roles.indexOf(envRole) >= 0) {
              _.safePush($scope.groupsByRole, envRole, groupMap[groupId]);
            }
          });
        });
      }

      // Initialze the map of users.
      if (usernames.length > 0) {
        userServices.get([], angular.toJson(usernames), function(usersResults) {
          var data = usersResults.data;
          for (var i = 0; i < data.length; i++) {
            userMap[data[i].username] = data[i];
          }

          _.each($scope.application.userRoles, function(roles, userId) {
            // lets put user ids by roles
            _.each($scope.appRoles, function(appRole) {
              if(roles.indexOf(appRole) >= 0) {
                _.safePush($scope.usersByRole, appRole, userMap[userId]);
              }
            });
          });
          updateUserEnvironmentRoles();
        });
      }
      // Initialze map of groups
      if (groupIds.length > 0) {
        groupServices.getMultiple([], angular.toJson(groupIds), function(groupsResults) {
          var data = groupsResults.data;
          for (var i = 0; i < data.length; i++) {
            groupMap[data[i].id] = data[i];
          }

          _.each($scope.application.groupRoles, function(roles, groupId) {
            // lets put user ids by roles
            _.each($scope.appRoles, function(appRole) {
              if(roles.indexOf(appRole) >= 0) {
                _.safePush($scope.groupsByRole, appRole, groupMap[groupId]);
              }
            });
          });

          updateGroupEnvironmentRoles();
        });
      }

      /** FOR USER SEARCH AND ADD APPLICATION'S ROLE */
      var updateRoles = function(roles, role, operation, target, targetElement) {
        switch (operation) {
          case 'add':
            if (!roles) {
              roles = [];
            }
            roles.push(role);
            _.safePush(target, role, targetElement);
            return roles;
          case 'remove':
            var index = roles.indexOf(role);
            roles.splice(index, 1);
            _.remove(target[role], function(element) {
              if(_.defined(element.username)) {
                return element.username === targetElement.username;
              }
              return element.name === targetElement.name;
            });
            return roles;
          default:
            break;
        }
      };

      // switch environment
      $scope.changeUserEnvironment = function(switchToEnvironment) {
        $scope.selectedEnvironment = switchToEnvironment;
        updateUserEnvironmentRoles();
        updateGroupEnvironmentRoles();
      };

      // Handle selection for USER
      $scope.handleAppRoleSelectionForUser = function(user, role) {
        var appUserRoles = $scope.application.userRoles[user.username];

        if (!appUserRoles || appUserRoles.indexOf(role) < 0) {
          // Add the role
          applicationServices.userRoles.addUserRole([], {
            applicationId: $scope.application.id,
            username: user.username,
            role: role
          }, function() {
            if (!userMap[user.username]) {
              userMap[user.username] = user;
            }
            $scope.application.userRoles[user.username] = updateRoles(appUserRoles, role, 'add', $scope.usersByRole, user);
          });

        } else {
          // remove the role
          applicationServices.userRoles.removeUserRole([], {
            applicationId: $scope.application.id,
            username: user.username,
            role: role
          }, function() {
            $scope.application.userRoles[user.username] = updateRoles(appUserRoles, role, 'remove', $scope.usersByRole, user);
          });
        }
      };

      $scope.handleEnvRoleSelectionForUser = function(user, role) {
        var envUserRoles = $scope.selectedEnvironment.userRoles[user.username];
        var envId = $scope.selectedEnvironment.id;
        if (!envUserRoles || envUserRoles.indexOf(role) < 0) {

          applicationEnvironmentServices.userRoles.addUserRole([], {
            applicationEnvironmentId: envId,
            applicationId: $scope.application.id,
            username: user.username,
            role: role
          }, function() {
            if (!userMap[user.username]) {
              userMap[user.username] = user;
            }
            $scope.selectedEnvironment.userRoles[user.username] = updateRoles(envUserRoles, role, 'add', $scope.usersByRole, user);
          });

        } else {
          applicationEnvironmentServices.userRoles.removeUserRole([], {
            applicationEnvironmentId: envId,
            applicationId: $scope.application.id,
            username: user.username,
            role: role
          }, function() {
            $scope.selectedEnvironment.userRoles[user.username] = updateRoles(envUserRoles, role, 'remove', $scope.usersByRole, user);
          });
        }
      };

      // Handle selection for GROUP
      $scope.handleAppRoleSelectionForGroup = function(group, role) {
        var appGroupRoles = $scope.application.groupRoles[group.id];
        if (!appGroupRoles || appGroupRoles.indexOf(role) < 0) {
          applicationServices.groupRoles.addGroupRole([], {
            applicationId: $scope.application.id,
            groupId: group.id,
            role: role
          }, function() {
            $scope.application.groupRoles[group.id] = updateRoles(appGroupRoles, role, 'add', $scope.groupsByRole, group);
            if (!groupMap[group.id]) {
              groupMap[group.id] = group;
            }
          });

        } else {
          applicationServices.groupRoles.removeGroupRole([], {
            applicationId: $scope.application.id,
            groupId: group.id,
            role: role
          }, function() {
            $scope.application.groupRoles[group.id] = updateRoles(appGroupRoles, role, 'remove', $scope.groupsByRole, group);
          });
        }
      };

      $scope.handleEnvRoleSelectionForGroup = function(group, role) {
        var envGroupRoles = $scope.selectedEnvironment.groupRoles[group.id];
        var envId = $scope.selectedEnvironment.id;
        if (!envGroupRoles || envGroupRoles.indexOf(role) < 0) {
          applicationEnvironmentServices.groupRoles.addGroupRole([], {
            applicationEnvironmentId: envId,
            applicationId: $scope.application.id,
            groupId: group.id,
            role: role
          }, function() {
            $scope.selectedEnvironment.groupRoles[group.id] = updateRoles(envGroupRoles, role, 'add', $scope.groupsByRole, group);
            if (!groupMap[group.id]) {
              groupMap[group.id] = group;
            }
          });

        } else {
          applicationEnvironmentServices.groupRoles.removeGroupRole([], {
            applicationEnvironmentId: envId,
            applicationId: $scope.application.id,
            groupId: group.id,
            role: role
          }, function() {
            $scope.selectedEnvironment.groupRoles[group.id] = updateRoles(envGroupRoles, role, 'remove', $scope.groupsByRole, group);
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
});
