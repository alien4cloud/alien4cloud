/* global UTILS */

'use strict';

var NewUserCtrl = ['$scope', '$modalInstance', 'userServices', function($scope, $modalInstance, userServices) {
  $scope.user = {};
  $scope.alienRoles = userServices.getAlienRoles();
  $scope.create = function(valid) {
    if (valid) {
      $modalInstance.close($scope.user);
    }
  };

  $scope.cancel = function() {
    $modalInstance.dismiss('cancel');
  };

  $scope.handleRoleSelection = function(user, role) {
    if (!user.roles || user.roles.indexOf(role) < 0) {
      userServices.addToRoleArray(user, role);
    } else {
      userServices.removeFromRoleArray(user, role);
    }
  };
}];

angular.module('alienUiApp').controller('UsersDirectiveCtrl', ['$scope', '$rootScope', '$modal', 'userServices', 'searchServiceFactory', 'groupServices',
    function($scope, $rootScope, $modal, userServices, searchServiceFactory, groupServices) {

      $scope.query = '';
      $scope.onSearchCompleted = function(searchResult) {
        $scope.usersData = searchResult.data;
        for (var i = 0; i < $scope.usersData.data.length; i++) {
          var user = $scope.usersData.data[i];
          userServices.initRolesToDisplay(user);
        }
      };
      $scope.searchService = searchServiceFactory('rest/users/search', false, $scope, 20);
      $scope.searchService.search();

      /** handle Modal form for user creation */
      $scope.openNewUserModal = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/users/new_user.html',
          controller: NewUserCtrl
        });

        modalInstance.result.then(function(newUser) {
          userServices.create([], angular.toJson(newUser), function() {
            $scope.searchService.search();
          });
        });
      };

      //prevent closing when clicking on a role
      $scope.preventClose = function(event) {
        event.stopPropagation();
      };

      //check if a app role is selected for a user
      $scope.checkIfAppRoleSelected = function(user, role) {
        console.log('check if App Role Selected', user, role);
        if ($scope.checkAppRoleSelectedCallback) {
          return $scope.checkAppRoleSelectedCallback({
            user: user,
            role: role
          });
        } else {
          //default checker
          if (user.roles) {
            return user.roles.indexOf(role) > -1;
          }
        }
        //return false either
        return false;
      };

      $scope.checkAppIfGroupSelected = function(user, group) {
        return UTILS.arrayContains(user.groups, group);
      };

      //check if a env role is selected for a user
      $scope.checkIfEnvRoleSelected = function(user, role) {
        if ($scope.checkEnvRoleSelectedCallback) {
          return $scope.checkEnvRoleSelectedCallback({
            user: user,
            role: role
          });
        } else {
          //default checker
          if (user.roles) {
            return user.roles.indexOf(role) > -1;
          }
        }
        //return false either
        return false;
      };

      $scope.checkIfEnvGroupSelected = function(user, group) {
        return UTILS.arrayContains(user.groups, group);
      };

      $scope.$watch('managedAppRoleList', function(newVal) {
        if (!newVal) {
          return;
        }
        $scope.searchService.search();
      });

      /*get groups*/
      $scope.searchGroups = function(groupQuery) {
        var searchRequest = {
          query: groupQuery,
          from: 0,
          size: 20
        };
        groupServices.search([], angular.toJson(searchRequest), function(results) {
          $scope.tempGroups = results.data.data;
          $scope.groups = [];
          $scope.groupsMap = {};
          $scope.tempGroups.forEach(function(group) {
            $scope.groupsMap[group.id] = group;
            // remove UTILS.ALL_USERS_GROUP
            if (group.name != UTILS.ALL_USERS_GROUP) {
              $scope.groups.push(group);
            }
          });
        });
      };

      $scope.searchGroups();

      $scope.filteredGroups = function(groups, user) {
        if (UTILS.isUndefinedOrNull(user.groups) || UTILS.isUndefinedOrNull(groups)) {
          return groups;
        }
        var filteredGroups = [];
        for (var int = 0; int < groups.length; int++) {
          if (!UTILS.arrayContains(user.groups, groups[int].name)) {
            filteredGroups.push(groups[int]);
          }
        }
        return filteredGroups;
      };

      $rootScope.$on('groupsChanged', function() {
        $scope.mustRefreshUsers = true;
      });

      $rootScope.$on('usersViewActive', function() {
        if ($scope.mustRefreshUsers) {
          $scope.searchService.search();
          $scope.searchGroups();
          $scope.mustRefreshUsers = false;
        }
      });

      $scope.userChanged = function(user, fieldName, fieldValue) {
        var updateUserRequest = {};
        updateUserRequest[fieldName] = fieldValue;
        userServices.update({
          username: user.username
        }, angular.toJson(updateUserRequest));
      };

      $scope.remove = function(user) {
        userServices.remove({
          username: user.username
        }, function() {
          $scope.searchService.search();
        });
      };
    }
  ])
  .directive('valueMatch', function() {
    return {
      require: 'ngModel',
      restrict: 'A',
      scope: {
        valueMatch: '='
      },
      link: function(scope, elem, attrs, ctrl) {
        scope.$watch(function() {
          return (ctrl.$pristine && angular.isUndefined(ctrl.$modelValue)) || scope.valueMatch === ctrl.$modelValue;
        }, function(currentValue) {
          ctrl.$setValidity('valueMatch', currentValue);
        });
      }
    };
  });
