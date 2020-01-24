define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  require('scripts/users/directives/groups');
  require('scripts/users/directives/users');
  require('scripts/common/directives/delete_confirm');

  // register state for user management.
  states.state('admin.users', {
    url: '/users',
    templateUrl: 'views/users/user_list.html',
    controller: 'UsersCtrl',
    menu: {
      id: 'am.admin.users',
      state: 'admin.users',
      key: 'NAVADMIN.MENU_USERS',
      icon: 'fa fa-users',
      priority: 100
    }
  });

  modules.get('a4c-security', ['a4c-common']).controller('UsersCtrl', ['$scope', '$rootScope', 'userServices', 'groupServices',
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
            if (_.undefined(result.error)) {
              if (_.undefined(group.roles)) {
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
            if (_.undefined(result.error)) {
              if (_.defined(group.roles)) {
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
        if (_.includes(user.groups, groupId)) {
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
});
