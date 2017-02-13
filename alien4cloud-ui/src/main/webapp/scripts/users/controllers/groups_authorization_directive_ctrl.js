define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/users/services/group_services');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');

  modules.get('a4c-security', ['a4c-search']).controller('GroupsAuthorizationDirectiveCtrl', ['$scope', '$uibModal',
    function ($scope, $uibModal) {
      // do nothin if there is no resource
      if(_.undefined($scope.resource)){
        return;
      }
      var refreshAuthorizedGroups = function (response) {
        $scope.authorizedGroups = response.data;
      };
      $scope.searchAuthorizedGroups = function () {
        $scope.service.get({}, refreshAuthorizedGroups);
      };
      $scope.searchAuthorizedGroups();

      $scope.openNewGroupAuthorizationModal = function () {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/users/groups_authorization_popup.html',
          controller: 'GroupsAuthorizationModalCtrl',
          resolve:{
            searchConfig:  $scope.buildSearchConfig(),
            authorizedGroups: function(){ return $scope.authorizedGroups; }
          }
        });

        modalInstance.result.then(function (groups) {
          $scope.service.save({}, _.map(groups, function (group) {
            return group.id;
          }), refreshAuthorizedGroups);
        });
      };

      $scope.revoke = function (group) {
        $scope.service.delete({
          groupId: group.id
        }, refreshAuthorizedGroups);
      };

      $scope.$watch('resource.id', function(newValue, oldValue){
        if(newValue === oldValue){
          return;
        }
        $scope.searchAuthorizedGroups();
      });

    }
  ]);
});
