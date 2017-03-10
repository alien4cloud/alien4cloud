define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/applications/services/application_environment_services');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');

  modules.get('a4c-security', ['a4c-search']).controller('AppsAuthorizationDirectiveCtrl', ['$scope',
    function ($scope) {
      // do nothin if there is no resource
      if(_.undefined($scope.resource)){
        return;
      }

      $scope.searchAuthorizedEnvironmentsPerApplication = function () {
        $scope.envService.get({}, function (response) {
          $scope.authorizedEnvironmentsPerApplication = response.data;
        });
      };
      $scope.searchAuthorizedEnvironmentsPerApplication();

      $scope.onModalClose = function(result){
        $scope.envService.save(result.subjects, $scope.searchAuthorizedEnvironmentsPerApplication);
      };

      $scope.revoke = function (application) {
        // here if application has env or not then do different things
        $scope.appService.delete({
          applicationId: application.application.id
        }, $scope.searchAuthorizedEnvironmentsPerApplication);
      };

      $scope.$watch('resource.id', function(newValue, oldValue){
        if(newValue === oldValue){
          return;
        }
        $scope.searchAuthorizedEnvironmentsPerApplication();
      });
    }
  ]);
});
