define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  modules.get('a4c-auth', ['ngResource']).factory('authService', ['$resource', '$location', '$state', '$http',
    function($resource, $location, $state, $http) {
      var userStatusResource = $resource('rest/latest/auth/status', {}, {
        'query': {
          method: 'GET',
          isArray: false
        }
      });
      // get default 'all-users' group
      var defaultAllUsersGroup = $resource('rest/latest/auth/groups/allusers', {}, {
        'query': {
          method: 'GET',
          isArray: false
        }
      }).query();

      /* Permissions */
      var allAccessAdminRole = 'ADMIN';

      return {
        currentStatus: null,
        menu: states.rootMenu(),

        onCurrentStatus: function() {
          var self = this;
          _.each(this.menu, function(menuItem) {
            menuItem.hasRole = self.hasOneRoleIn(menuItem.roles);
          });
        },

        getStatus: function() {
          var self = this;
          if (this.currentStatus === null) {
            this.currentStatus = userStatusResource.query();
            this.currentStatus.$promise.then(function() {
              self.onCurrentStatus();
            });
          }
          return this.currentStatus;
        },

        authorizationCheck: function(checkCallback) {
          var self = this;
          if (typeof this.currentStatus.data !== 'undefined') {
            return checkCallback(this.currentStatus.data);
          } else {
            return this.getStatus().$promise.then(
              function() {
                return checkCallback(self.currentStatus.data);
              }
            );
          }
        },

        /** Checks if the user has one of the role requested. */
        hasOneRoleIn: function(roles) {
          if (!_.isEmpty(roles)) {
            return this.authorizationCheck(
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
        },

        getRolesForResource: function(resource, userStatus) {
          var allRoles;
          var userRoles = _.get(resource, 'userRoles["' + userStatus.username + '"]', []);
          if (!_.isEmpty(userRoles)) {
            allRoles = userRoles;
          }
          var groups = userStatus.groups;
          if (_.undefined(groups)) {
            groups = [];
          }
          if (_.defined(defaultAllUsersGroup.data)) {
            groups.push(defaultAllUsersGroup.data.id);
          }
          var groupRolesMap = resource.groupRoles;
          if (_.defined(groups) && !_.isEmpty(groups) && _.defined(groupRolesMap) && !_.isEmpty(groupRolesMap)) {
            for (var i = 0; i < groups.length; i++) {
              var group = groups[i];
              if (groupRolesMap.hasOwnProperty(group)) {
                allRoles = _.union(allRoles, groupRolesMap[group]);
              }
            }
          }
          return _.defined(allRoles) ? allRoles : [];
        },

        /** Check if a user has access to a resource. Return true if user has at least one of the given roles **/
        hasResourceRoleIn: function(resource, roles) {
          var self = this;
          return this.authorizationCheck(
            function(userStatus) {
              // check all accessible roles first
              if (userStatus.roles.indexOf(allAccessAdminRole) > -1) {
                return true;
              }
              // check if the user has the role.
              var currentUserRoles = self.getRolesForResource(resource, userStatus);
              for (var i = 0; i < roles.length; i++) {
                if (currentUserRoles.indexOf(roles[i]) >= 0) {
                  return true;
                }
              }
              return false;
            });
        },

        /**
         * LogOut and redirect to home.
         */
        logOut: function() {
          this.currentStatus = null;
          window.location.href = '/logout';
        },
        /**
         * Login and redirect to target
         */
        logIn: function(data, scope) {
          var self = this;
          $http.post('login', data, {
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          }).then(function() {
            self.currentStatus = userStatusResource.query(function(loginResult) {
              // get the new status from server
              if (loginResult.data.isLogged === false) {
                var loginError = {
                  'show': true,
                  'data': 'Authentication error. Invalid username or password.'
                };
                scope.error = loginError;
              }
              $state.go('home', {}, {
                reload: true
              });
            });
            self.currentStatus.$promise.then(function() {
              self.onCurrentStatus();
            });

            return self.currentStatus;
          }).catch(function() {
            self.currentStatus = {
              'data': {
                'isLogged': false
              }
            };
          });
        },

        /**
         * Checks if the current user has the requested role.
         */
        hasRole: function(role) {
          return this.hasOneRoleIn([role]);
        },

        /**
         * Check if a user has access to a resource. Return true if user has the given role
         */
        hasResourceRole: function(resource, role) {
          return this.hasResourceRoleIn(resource, [role]);
        }
      };
    }
  ]);
});
