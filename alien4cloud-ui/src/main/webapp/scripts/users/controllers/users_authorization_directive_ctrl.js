define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');

  require('scripts/users/services/user_services');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');

  var NewUserAuthorizationController = ['$scope', '$uibModalInstance', '$resource', '$state', 'locationSecurityService',
    function($scope, $uibModalInstance, $resource, $state, locationSecurityService) {
      // $scope.create = function(valid, envType, version) {
      //   if (valid) {
      //     // prepare the good request
      //     var applicationId = $state.params.id;
      //     $scope.environment.applicationId = applicationId;
      //     $scope.environment.environmentType = envType;
      //     $scope.environment.versionId = version;
      //     $uibModalInstance.close($scope.environment);
      //   }
      // };
      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];

  modules.get('a4c-security', ['a4c-search']).controller('UsersAuthorizationDirectiveCtrl', ['$scope', '$rootScope', '$uibModal', 'userServices', 'searchServiceFactory', 'groupServices',
    function($scope, $rootScope, $uibModal, userServices, searchServiceFactory, groupServices) {

      $scope.query = '';
      $scope.onSearchCompleted = function(searchResult) {
        $scope.usersData = searchResult.data;
        for (var i = 0; i < $scope.usersData.data.length; i++) {
          var user = $scope.usersData.data[i];
          userServices.initRolesToDisplay(user);
        }
      };
      $scope.searchService = searchServiceFactory('rest/latest/users/search', false, $scope, 20);
      $scope.searchService.search();

      $scope.searchAuthorizedUsers = function() {
        $scope.authorizedUsers = locationSecurityService.getLocationUsers();
      }


      $scope.openNewUserAuthorizationModal = function() {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/users/users_authorization_popup.html',
          controller: NewUserAuthorizationController
        });

        // modalInstance.result.then(function(newLocation) {
        //   locationService.create({orchestratorId: orchestrator.id}, angular.toJson(newLocation), function() {
        //     updateLocations();
        //   });
        // });
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
            // remove groupServices.ALL_USERS_GROUP
            if (group.name !== groupServices.ALL_USERS_GROUP) {
              $scope.groups.push(group);
            }
          });
        });
      };

      $scope.searchGroups();

      $scope.filteredGroups = function(groups, user) {
        if (_.undefined(user.groups) || _.undefined(groups)) {
          return groups;
        }
        var filteredGroups = [];
        for (var int = 0; int < groups.length; int++) {
          if (!_.contains(user.groups, groups[int].name)) {
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
});
