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

      // // get all orchestrator assignable roles
      $resource('rest/latest/auth/roles/deployer', {}, {
        method: 'GET'
      }).get().$promise.then(function(roleResult) {
        $scope.orchestratorRoles = roleResult.data;
      });

      $scope.relatedUsers = {};
      if ($scope.orchestrator.authorizedUsers) {
        var usernames = [];
        for (var i in $scope.orchestrator.authorizedUsers) {
          var username = $scope.orchestrator.authorizedUsers[i];
          if ($scope.orchestrator.authorizedUsers.hasOwnProperty(username)) {
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
      if ($scope.orchestrator.authorizedGroups) {
        var groupIds = [];
        for (var j in $scope.orchestrator.authorizedGroups) {
          var groupId = $scope.orchestrator.authorizedGroups[j];
          if ($scope.orchestrator.authorizedGroups.hasOwnProperty(groupId)) {
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

      // Handle orchestrator security action
      $scope.handleRoleSelectionForUser = function(user, role) {
        if (_.undefined($scope.orchestrator.authorizedUsers)) {
          $scope.orchestrator.authorizedUsers = [];
        }
        if ($scope.orchestrator.authorizedUsers.indexOf(user.username) < 0) {
          orchestratorSecurityService.userRoles.addUserRole([], {
            id: $scope.orchestrator.id,
            username: user.username,
            role: role
          }, function() {
            $scope.orchestrator.authorizedUsers.push(user.username);
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
            _.pull($scope.orchestrator.authorizedUsers, user.username);
          });
        }
      };

      $scope.handleRoleSelectionForGroup = function(group, role) {
        if (_.undefined($scope.orchestrator.authorizedGroups)) {
          $scope.orchestrator.authorizedGroups = [];
        }
        if ($scope.orchestrator.authorizedGroups.indexOf(group.id) < 0) {
          orchestratorSecurityService.groupRoles.addGroupRole([], {
            id: $scope.orchestrator.id,
            groupId: group.id,
            role: role
          }, function() {
            $scope.orchestrator.authorizedGroups.push(group.id);
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
            _.pull($scope.orchestrator.authorizedGroups, group.id);
          });
        }
      };

      $scope.checkOrchestratorRoleSelectedForUser = function(user) {
        if ($scope.orchestrator && $scope.orchestrator.authorizedUsers) {
          return $scope.orchestrator.authorizedUsers.indexOf(user.username) > -1;
        }
        return false;
      };

      $scope.checkOrchestratorRoleSelectedForGroup = function(group) {
        if ($scope.orchestrator && $scope.orchestrator.authorizedGroups) {
          return $scope.orchestrator.authorizedGroups.indexOf(group.id) > -1;
        }
        return false;
      };
    }
  ]); // controller
}); // define
