'use strict';

angular.module('alienAuth', ['ngResource', 'pascalprecht.translate'], ['$provide',
  function($provide) {
    $provide.factory('alienAuthService', ['$resource', '$state', '$http', 'alienNavBarService', 'groupServices',
      function($resource, $state, $http, alienNavBarService, groupServices) {
        // isArray needed when results is JSON with nested object
        var userStatusResource = $resource('rest/auth/status', {}, {
          'query': {
            method: 'GET',
            isArray: false
          }
        });

        // get default allusers group
        var defaultAllUsersGroup = $resource('rest/auth/groups/allusers', {}, {
          'query': {
            method: 'GET',
            isArray: false
          }
        }).query();

        var onCurrentStatus = function() {
          for (var i = 0; i < alienNavBarService.menu.left.length; i++) {
            alienNavBarService.menu.left[i].hasRole = hasOneRoleIn(alienNavBarService.menu.left[i].roles);
          }
          for (var j = 0; j < alienNavBarService.menu.right.length; j++) {
            alienNavBarService.menu.right[j].hasRole = hasOneRoleIn(alienNavBarService.menu.right[j].roles);
          }
        };

        var getStatus = function() {
          if (this.currentStatus === null) {
            this.currentStatus = this.userStatusResource.query();
            var obj = this;
            this.currentStatus.$promise.then(function() {
              obj.onCurrentStatus();
            });
          }
          return this.currentStatus;
        };

        // LogOut and redirect to root.
        var logOut = function() {
          var obj = this;
          $http.post('logout').success(function() {
            obj.currentStatus = obj.userStatusResource.query();
            $state.go('home');
          });
        };

        // Login and redirect to target
        var logIn = function(data, scope) {
          var obj = this;
          $http.post('login', data, {
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          }).success(function() {
            obj.currentStatus = null;
            var newStatus = obj.userStatusResource.query(function(loginResult) {
              // get the new status from server
              if (loginResult.data.isLogged === false) {
                var loginError = {
                  'show': true,
                  'data': 'Authentication error. Invalid username or password.'
                };
                scope.error = loginError;
                $state.go('home');
              } else {
                $state.go('home_user');
              }
            });

            obj.currentStatus = newStatus;
            obj.currentStatus.$promise.then(function() {
              obj.onCurrentStatus();
            });

            return newStatus;
          }).error(function() {
            obj.currentStatus = {
              'data': {
                'isLogged': false
              }
            };
          });
        };

        /* Permissions */
        var allAccessAdminRole = 'ADMIN';
        var authorizationCheck = function(checkCallback) {
          if (typeof authService.currentStatus.data !== 'undefined') {
            return checkCallback(authService.currentStatus.data);
          } else {
            return authService.getStatus().$promise.then(
              function() {
                return checkCallback(authService.currentStatus.data);
              }
            );
          }
        };

        /* Checks if the current user has the requested role. */
        var hasRole = function(role) {
          return hasOneRoleIn([role]);
        };

        /** Checks if the user has one of the role requested. */
        var hasOneRoleIn = function(roles) {
          if (UTILS.isArrayDefinedAndNotEmpty(roles)) {
            return authorizationCheck(
              function(userData) {
                if (!userData.isLogged) {
                  return false;
                }
                if (userData.roles.indexOf(allAccessAdminRole) > -1) {
                  return true;
                }
                for (var i = 0; i < roles.length; i++) {
                  var hasRole = userData.roles.indexOf(roles[i]) > -1;
                  if (hasRole) {
                    return true;
                  }
                }
                return false;
              }
            );
          } else {
            // empty roles array => full access for authenticated user at least
            return true;
          }

        };

        /** Check if a user has access to a resource. Return true if user has the given role **/
        var hasResourceRole = function(resource, role) {
          return hasResourceRoleIn(resource, [role]);
        };

        var getRolesForResource = function(resource, userStatus) {
          var allRoles;
          var userRoles = resource.userRoles[userStatus.username];
          if (UTILS.isArrayDefinedAndNotEmpty(userRoles)) {
            allRoles = userRoles;
          }
          var groups = userStatus.groups;
          var groupRolesMap = resource.groupRoles;
          if (UTILS.isArrayDefinedAndNotEmpty(groups) && UTILS.isDefinedAndNotNull(groupRolesMap) && !UTILS.isObjectEmpty(groupRolesMap)) {
            for (var i = 0; i < groups.length; i++) {
              var group = groups[i];
              if (groupRolesMap.hasOwnProperty(group)) {
                allRoles = UTILS.concat(allRoles, groupRolesMap[group]);
              }
            }
          }
          return UTILS.isDefinedAndNotNull(allRoles) ? allRoles : [];
        };

        /** Check if a user has access to a resource. Return true if user has at least one of the given roles **/
        var hasResourceRoleIn = function(resource, roles) {
          return authorizationCheck(
            function(userStatus) {
              // check all accessible roles first
              if (userStatus.roles.indexOf(allAccessAdminRole) > -1) {
                return true;
              }
              // check if the resource has ALL_USERS group rights
              if (UTILS.isDefinedAndNotNull(defaultAllUsersGroup.data) && resource.groupRoles.hasOwnProperty(defaultAllUsersGroup.data.id)) {
                return true;
              }
              // check if the user has the role.
              var currentUserRoles = getRolesForResource(resource, userStatus);
              for (var i = 0; i < roles.length; i++) {
                if (currentUserRoles.indexOf(roles[i]) >= 0) {
                  return true;
                }
              }
              return false;
            });
        };

        var authService = {
          'logIn': logIn,
          'logOut': logOut,
          'getStatus': getStatus,
          'currentStatus': null,
          'currentUser': null,
          'onCurrentStatus': onCurrentStatus,
          'userStatusResource': userStatusResource,
          'hasRole': hasRole,
          'hasOneRoleIn': hasOneRoleIn,
          'hasResourceRole': hasResourceRole,
          'hasResourceRoleIn': hasResourceRoleIn
        };
        return authService;
      }
    ]);
  }
]);
