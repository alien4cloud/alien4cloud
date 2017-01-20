define(function (require) {
  'use strict';
  
  var modules = require('modules');
  var _ = require('lodash');
  
  require('scripts/users/services/group_services');
  require('scripts/orchestrators/services/location_security_service');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');
  
  var NewGroupAuthorizationController = ['$scope', '$uibModalInstance', 'searchServiceFactory',
    function ($scope, $uibModalInstance, searchServiceFactory) {
      $scope.batchSize = 5;
      $scope.selectedGroups = [];
      $scope.query = '';
      $scope.onSearchCompleted = function (searchResult) {
        $scope.selectedGroups = [];
        $scope.groupsData = searchResult.data;
      };
      $scope.searchService = searchServiceFactory('rest/latest/groups/search', false, $scope, $scope.batchSize);
      $scope.searchService.search();
      
      $scope.ok = function () {
        if ($scope.selectedGroups.length > 0) {
          $uibModalInstance.close($scope.selectedGroups);
        }
      };
      
      $scope.toggleSelection = function (group) {
        var indexOfGroupInSelected = $scope.selectedGroups.indexOf(group);
        if (indexOfGroupInSelected < 0) {
          $scope.selectedGroups.push(group);
        } else {
          $scope.selectedGroups.splice(indexOfGroupInSelected, 1);
        }
      };
      
      $scope.isSelected = function (group) {
        return $scope.selectedGroups.indexOf(group) >= 0;
      };
      
      $scope.toggleSelectAll = function () {
        if ($scope.selectedGroups.length === $scope.groupsData.data.length) {
          $scope.selectedGroups = [];
        } else {
          $scope.selectedGroups = $scope.groupsData.data.slice();
        }
      };
      
      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];
  
  modules.get('a4c-security', ['a4c-search']).controller('GroupsAuthorizationDirectiveCtrl', ['$scope', '$uibModal', 'locationSecurityService',
    function ($scope, $uibModal, locationSecurityService) {
      $scope.searchAuthorizedGroups = function () {
        locationSecurityService.groups.get({
          orchestratorId: $scope.orchestrator.id,
          locationId: $scope.location.id
        }, function (response) {
          $scope.authorizedGroups = response.data;
        });
      };
      $scope.searchAuthorizedGroups();
      
      $scope.openNewGroupAuthorizationModal = function () {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/users/groups_authorization_popup.html',
          controller: NewGroupAuthorizationController
        });
        
        modalInstance.result.then(function (groups) {
          locationSecurityService.groups.save({
            orchestratorId: $scope.orchestrator.id,
            locationId: $scope.location.id
          }, _.map(groups, function (group) {
            return group.id;
          }), $scope.searchAuthorizedGroups);
        });
      };
      
      $scope.revoke = function (group) {
        locationSecurityService.groups.delete({
          orchestratorId: $scope.orchestrator.id,
          locationId: $scope.location.id,
          groupId: group.id
        }, $scope.searchAuthorizedGroups);
      };
    }
  ]);
});
