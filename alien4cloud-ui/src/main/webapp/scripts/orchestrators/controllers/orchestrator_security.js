define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/orchestrators/services/orchestrator_security_service');


  states.state('admin.orchestrators.details.security', {
    url: '/security',
    templateUrl: 'views/orchestrators/orchestrator_security.html',
    controller: 'OrchestratorSecurityCtrl',
    menu: {
      id: 'menu.orchestrators.security',
      state: 'admin.orchestrators.details.security',
      key: 'ORCHESTRATORS.NAV.AUTHORIZATIONS',
      icon: 'fa fa-users',
      priority: 600
    }
  });

  modules.get('a4c-orchestrators').controller('OrchestratorSecurityCtrl',
    ['$scope', '$modal', '$state', '$resource', 'orchestrator', 'orchestratorSecurityService', 'userServices', 'groupServices',
    function($scope, $modal, $state, $resource, orchestrator, orchestratorSecurityService, userServices, groupServices) {
      $scope.orchestrator = orchestrator;

      // get all orchestrator assignable roles
      $resource('rest/auth/roles/orchestrator', {}, {
        method: 'GET'
      }).get().$promise.then(function(roleResult) {
        $scope.orchestratorRoles = roleResult.data;
      });

      $scope.relatedUsers = {};
      if ($scope.orchestrator.userRoles) {
        var usernames = [];
        for (var username in $scope.orchestrator.userRoles) {
          if ($scope.orchestrator.userRoles.hasOwnProperty(username)) {
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
      if ($scope.orchestrator.groupRoles) {
        var groupIds = [];
        for (var groupId in $scope.orchestrator.groupRoles) {
          if ($scope.orchestrator.groupRoles.hasOwnProperty(groupId)) {
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

      // Handle orchestrator security action
      $scope.handleRoleSelectionForUser = function(user, role) {
        if (_.undefined($scope.orchestrator.userRoles)) {
          $scope.orchestrator.userRoles = {};
        }
        var orchestratorUserRoles = $scope.orchestrator.userRoles[user.username];
        if (!orchestratorUserRoles || orchestratorUserRoles.indexOf(role) < 0) {
          orchestratorSecurityService.userRoles.addUserRole([], {
            id: $scope.orchestrator.id,
            username: user.username,
            role: role
          }, function() {
            $scope.orchestrator.userRoles[user.username] = updateRoles(orchestratorUserRoles, role, 'add');
            if (!$scope.relatedUsers[user.username]) {
              $scope.relatedUsers[user.username] = user;
            }
          });
        } else {
          orchestratorSecurityService.userRoles.removeUserRole([], {
            id: $scope.orchestrator.id,
            username: user.username,
            role: role
          }, function() {
            $scope.orchestrator.userRoles[user.username] = updateRoles(orchestratorUserRoles, role, 'remove');
          });
        }
      };

      $scope.handleRoleSelectionForGroup = function(group, role) {
        if (_.undefined($scope.orchestrator.groupRoles)) {
          $scope.orchestrator.groupRoles = {};
        }
        var orchestratorGroupRoles = $scope.orchestrator.groupRoles[group.id];
        if (!orchestratorGroupRoles || orchestratorGroupRoles.indexOf(role) < 0) {
          orchestratorSecurityService.groupRoles.addGroupRole([], {
            id: $scope.orchestrator.id,
            groupId: group.id,
            role: role
          }, function() {
            $scope.orchestrator.groupRoles[group.id] = updateRoles(orchestratorGroupRoles, role, 'add');
            if (!$scope.relatedGroups[group.id]) {
              $scope.relatedGroups[group.id] = group;
            }
          });
        } else {
          orchestratorSecurityService.groupRoles.removeGroupRole([], {
            id: $scope.orchestrator.id,
            groupId: group.id,
            role: role
          }, function() {
            $scope.orchestrator.groupRoles[group.id] = updateRoles(orchestratorGroupRoles, role, 'remove');
          });
        }
      };

      $scope.checkOrchestratorRoleSelectedForUser = function(user, role) {
        if ($scope.orchestrator && $scope.orchestrator.userRoles && $scope.orchestrator.userRoles[user.username]) {
          return $scope.orchestrator.userRoles[user.username].indexOf(role) > -1;
        }
        return false;
      };

      $scope.checkOrchestratorRoleSelectedForGroup = function(group, role) {
        if ($scope.orchestrator && $scope.orchestrator.groupRoles && $scope.orchestrator.groupRoles[group.id]) {
          return $scope.orchestrator.groupRoles[group.id].indexOf(role) > -1;
        }
        return false;
      };
    }
  ]); // controller
}); // define
