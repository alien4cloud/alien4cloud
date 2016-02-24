define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/services/location_security_service');

  states.state('admin.orchestrators.details.locations.security', {
    url: '/security',
    templateUrl: 'views/orchestrators/orchestrator_location_security.html',
    controller: 'OrchestratorLocationsSecurityCtrl',
    menu: {
      id: 'menu.orchestrators.locations.security',
      state: 'admin.orchestrators.details.locations.security',
      key: 'ORCHESTRATORS.LOCATIONS.SECURITY',
      icon: 'fa fa-users',
      priority: 600
    }
  });

  modules.get('a4c-orchestrators', ['ui.router', 'ui.bootstrap','a4c-common']).controller('OrchestratorLocationsSecurityCtrl',
    ['$scope', 'orchestrator', '$resource', 'userServices', 'groupServices', 'locationSecurityService',
    function($scope, orchestrator, $resource, userServices, groupServices, locationSecurityService) {
      $scope.orchestrator = orchestrator;

      // get all location assignable roles
      $resource('rest/v1/auth/roles/deployer', {}, {
        method: 'GET'
      }).get().$promise.then(function(roleResult) {
        $scope.locationRoles = roleResult.data;
      });

      var refresh = function(){
        $scope.relatedUsers = {};
        if ($scope.context.location.userRoles) {
          var usernames = [];
          for (var username in $scope.context.location.userRoles) {
            if ($scope.context.location.userRoles.hasOwnProperty(username)) {
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
        if ($scope.context.location.groupRoles) {
          var groupIds = [];
          for (var groupId in $scope.context.location.groupRoles) {
            if ($scope.context.location.groupRoles.hasOwnProperty(groupId)) {
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
      }
      
      //first run 
      refresh();
      
      // Watch over selected location id to refresh
      $scope.$watch('context.location.id', function() {
        refresh();
      });

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

      // Handle location security action
      $scope.handleRoleSelectionForUser = function(user, role) {
        if (_.undefined($scope.context.location.userRoles)) {
          $scope.context.location.userRoles = {};
        }
        var locationUserRoles = $scope.context.location.userRoles[user.username];
        if (!locationUserRoles || locationUserRoles.indexOf(role) < 0) {
          locationSecurityService.userRoles.addUserRole([], {
            orchestratorId: $scope.orchestrator.id,
            locationId: $scope.context.location.id,
            username: user.username,
            role: role
          }, function() {
            $scope.context.location.userRoles[user.username] = updateRoles(locationUserRoles, role, 'add');
            if (!$scope.relatedUsers[user.username]) {
              $scope.relatedUsers[user.username] = user;
            }
          });
        } else {
          locationSecurityService.userRoles.removeUserRole([], {
            orchestratorId: $scope.orchestrator.id,
            locationId: $scope.context.location.id,
            username: user.username,
            role: role
          }, function() {
            $scope.context.location.userRoles[user.username] = updateRoles(locationUserRoles, role, 'remove');
          });
        }
      };

      $scope.handleRoleSelectionForGroup = function(group, role) {
        if (_.undefined($scope.context.location.groupRoles)) {
          $scope.context.location.groupRoles = {};
        }
        var locationGroupRoles = $scope.context.location.groupRoles[group.id];
        if (!locationGroupRoles || locationGroupRoles.indexOf(role) < 0) {
          locationSecurityService.groupRoles.addGroupRole([], {
            orchestratorId: $scope.orchestrator.id,
            locationId: $scope.context.location.id,
            groupId: group.id,
            role: role
          }, function() {
            $scope.context.location.groupRoles[group.id] = updateRoles(locationGroupRoles, role, 'add');
            if (!$scope.relatedGroups[group.id]) {
              $scope.relatedGroups[group.id] = group;
            }
          });
        } else {
          locationSecurityService.groupRoles.removeGroupRole([], {
            orchestratorId: $scope.orchestrator.id,
            locationId: $scope.context.location.id,
            groupId: group.id,
            role: role
          }, function() {
            $scope.context.location.groupRoles[group.id] = updateRoles(locationGroupRoles, role, 'remove');
          });
        }
      };

      $scope.checklocationRoleSelectedForUser = function(user, role) {
        if ($scope.context.location && $scope.context.location.userRoles && $scope.context.location.userRoles[user.username]) {
          return $scope.context.location.userRoles[user.username].indexOf(role) > -1;
        }
        return false;
      };

      $scope.checklocationRoleSelectedForGroup = function(group, role) {
        if ($scope.context.location && $scope.context.location.groupRoles && $scope.context.location.groupRoles[group.id]) {
          return $scope.context.location.groupRoles[group.id].indexOf(role) > -1;
        }
        return false;
      };

    }
  ]); // controller
}); // define
