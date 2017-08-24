/*
* Application list is the entry point for the application management.
*/
define(function (require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');
  var angular = require('angular');
  var _ = require('lodash');
  var d3 = require('d3');
  var alienUtils = require('scripts/utils/alien_utils');

  require('scripts/_ref/applications/controllers/applications_new');
  require('scripts/_ref/applications/controllers/applications_detail');
  require('scripts/applications/services/application_services');

  require('scripts/_ref/common/directives/search');

  require('scripts/common/filters/id_to_version');
  require('scripts/common/services/pie_chart_service.js');

  states.state('applications', {
    url: '/applications',
    template: '<ui-view/>',
    menu: {
      id: 'menu.applications',
      state: 'applications',
      key: 'NAVBAR.MENU_APPS',
      icon: 'fa fa-desktop',
      priority: 10
    }
  });
  states.state('applications.list', {
    url: '/list',
    templateUrl: 'views/_ref/applications/applications_list.html',
    controller: 'ApplicationListCtrl'
  });
  states.forward('applications', 'applications.list');

  modules.get('a4c-applications').controller('ApplicationListCtrl',
    ['$scope', '$state', '$uibModal', '$translate', 'toaster', 'pieChartService', 'authService', 'applicationServices', 'searchServiceFactory',
    function ($scope, $state, $uibModal, $translate, toaster, pieChartService, authService, applicationServices, searchServiceFactory) {
      $scope.fromStatusToCssClasses = alienUtils.getStatusIconCss;
      $scope.isManager = authService.hasRole('APPLICATIONS_MANAGER');
      $scope.queryManager = {};
      $scope.searchService = searchServiceFactory('rest/latest/applications/search', false, $scope.queryManager, 20);

      d3.selectAll('.d3-tip').remove();

      $scope.applicationEnvironmentMap = {};

      // Defines actions on application or application environment selection.
      $scope.openApplication = function (applicationId) {
        $state.go('applications.detail', {
          id: applicationId
        });
      };
      $scope.openDeploymentPage = function (application, environmentId) {
        $state.go('applications.detail.environment', {
          id: application.id,
          environmentId: environmentId
        });
      };

      $scope.removeApplication = function (applicationId) {
        applicationServices.remove({
          applicationId: applicationId
        }, function (response) {
          if (!response.error && response.data === true) {
            $scope.searchService.search();
          } else {
            toaster.pop('error', $translate.instant('APPLICATIONS.ERRORS.DELETE_TITLE'), $translate.instant('APPLICATIONS.ERRORS.DELETING_FAILED'), 4000, 'trustedHtml', null);
          }
        });
      };

      var getApplicationEnvironments = function (applications) {
        var requestEnvironments = [];
        Object.keys(applications).forEach(function (key) {
          requestEnvironments.push(applications[key].id);
        });
        var appStatuses = applicationServices.envrironments.getAll([], angular.toJson(requestEnvironments));
        return appStatuses;
      };

      var priorityToOrder = {
        'DEPLOYED': '8',
        'UNDEPLOYED': '7',
        'UNKNOWN': '6',
        'WARNING': '5',
        'FAILURE': '4',
        'DEPLOYMENT_IN_PROGRESS': '3',
        'UNDEPLOYMENT_IN_PROGRESS': '2'
      };

      var updateApplicationStatuses = function (applicationSearchResult) {
        if (!angular.isUndefined(applicationSearchResult)) {
          var environmentsQuery = getApplicationEnvironments(applicationSearchResult.data);
          environmentsQuery.$promise.then(function (environmentsResult) {
            $scope.applicationEnvironmentMap = environmentsResult.data;
            _.each(applicationSearchResult.data, function(app) {
              var data = [];
              _.each(environmentsResult.data[app.id], function(envDTO) {
                // segment in the env pie
                var segment = {};
                segment.label = envDTO.status;
                segment.color = alienUtils.getStatusColor(envDTO);
                segment.indexToOrder = priorityToOrder[envDTO.status];
                envDTO.indexToOrder = priorityToOrder[envDTO.status];
                segment.value = 1;
                segment.id = envDTO.id;
                segment.name = envDTO.name;

                // Initialize the counter of number deployed environment by applications to sort
                if (!_.isNumber(app.countDeployedEnvironment)) {
                  app.countDeployedEnvironment = 0;
                }

                if (segment.label === 'DEPLOYED' || segment.label === 'UPDATED') {
                  app.countDeployedEnvironment++;
                  app.isDeployed = true;
                }
                data.push(segment);

                envDTO.canDeploy = authService.hasResourceRole(app, 'APPLICATION_MANAGER') || authService.hasResourceRole(envDTO, 'DEPLOYMENT_MANAGER');
              });
              pieChartService.render(app.id, data);
            });
          });
        }
        return applicationSearchResult;
      };

      $scope.$watch('queryManager.searchResult', function (searchResult) {
        if(_.defined(searchResult)) {
          updateApplicationStatuses(searchResult);
        }
      });

      // New application creation
      $scope.onNewApp = function () {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/applications/application_new.html',
          controller: 'NewApplicationCtrl',
          windowClass: 'new-app-modal'
        });
        modalInstance.result.then(function (application) {
          // create a new application from the given name and description.
          applicationServices.create([], angular.toJson(application), function (result) {
            if (_.defined(result.error)) {
              toaster.pop('error', $translate.instant('ERRORS.' + result.error.code), $translate.instant('ERRORS.' + result.error.code), 6000, 'trustedHtml');
            } else {
              $scope.openApplication(result.data);
            }
          });
        });
      };
    }
  ]); // controller
});
