define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/applications/services/application_environment_services');
  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');

  modules.get('a4c-security', ['a4c-search']).controller('AppsAuthorizationDirectiveCtrl', ['$scope', '$uibModal',
    function ($scope, $uibModal) {
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

      $scope.openNewAppAuthorizationModal = function (app) {
        $scope.application = app;
        $scope.preSelection = {};
        $scope.preSelectedApps = {};
        $scope.preSelectedEnvs = {};
        _.forEach($scope.authorizedEnvironmentsPerApplication, function(authorizedApp) {
          if (_.isEmpty(authorizedApp.environments)) {
            $scope.preSelectedApps[authorizedApp.application.id] = 1;
          }
          $scope.preSelection[authorizedApp.application.id] = [];
          _.forEach(authorizedApp.environments, function(environment) {
            $scope.preSelectedEnvs[environment.id] = 1;
            $scope.preSelection[authorizedApp.application.id].push(environment.id);
          });
        });

        var modalInstance = $uibModal.open({
          templateUrl: 'views/users/apps_authorization_popup.html',
          controller: 'AppsAuthorizationModalCtrl',
          resolve:{
            searchConfig:  $scope.buildSearchConfig(),
            preSelection: $scope.preSelection,
            preSelectedApps:   $scope.preSelectedApps,
            preSelectedEnvs:   $scope.preSelectedEnvs
          },
          scope: $scope
        });

        modalInstance.result.then(function (result) {
          $scope.envService.save({}, result, $scope.searchAuthorizedEnvironmentsPerApplication);
        });
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
