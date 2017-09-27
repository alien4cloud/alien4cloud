define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var d3 = require('d3');
  var _ = require('lodash');
  var alienUtils = require('scripts/utils/alien_utils');

  require('scripts/common/services/pie_chart_service.js');
  require('scripts/applications/services/application_services');
  require('scripts/applications/controllers/application_detail');
  require('scripts/topology/services/topology_services');
  require('scripts/applications/directives/topology_init_from_select');

  require('scripts/common/filters/id_to_version');

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
    templateUrl: 'views/applications/application_list.html',
    controller: 'ApplicationListCtrl'
  });
  states.forward('applications', 'applications.list');

  var NewApplicationCtrl = ['$scope', '$uibModalInstance',
    function ($scope, $uibModalInstance) {
      $scope.app = {};
      $scope.namePattern=new RegExp('^[^\/\\\\]+$');
      $scope.archiveNamePattern=new RegExp('^\\w+$');
      $scope.fromIndex = 3;
      var autoGenArchiveName = true;
      $scope.nameChange = function () {
        if (autoGenArchiveName && $scope.app.name) {
          $scope.app.archiveName = _.capitalize(_.camelCase($scope.app.name));
        }
      };
      $scope.archiveNameChange = function () {
        autoGenArchiveName = false;
      };
      $scope.create = function (valid) {
        if (valid) {
          // if we create from template let's set the template id to the app.
          if($scope.fromIndex === 2) {
            $scope.app.topologyTemplateVersionId = $scope.topologyTemplate.versionId;
          }
          $uibModalInstance.close($scope.app);
        }
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ];

  modules.get('a4c-applications').controller('ApplicationListCtrl',
    ['$scope', '$uibModal', '$state', 'authService', 'applicationServices', '$translate', 'toaster', 'searchServiceFactory', 'pieChartService',
      function ($scope, $uibModal, $state, authService, applicationServices, $translate, toaster, searchServiceFactory, pieChartService) {
        $scope.isManager = authService.hasRole('APPLICATIONS_MANAGER');
        $scope.applicationEnvironmentMap = {};
        d3.selectAll('.d3-tip').remove();
        $scope.fromStatusToCssClasses = alienUtils.getStatusIconCss;

        $scope.openNewApp = function () {
          var modalInstance = $uibModal.open({
            templateUrl: 'views/applications/application_new.html',
            controller: NewApplicationCtrl,
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

        var getApplicationEnvironments = function (applications) {
          var requestEnvironments = [];
          Object.keys(applications).forEach(function (key) {
            requestEnvironments.push(applications[key].id);
          });
          var appStatuses = applicationServices.envrironments.getAll([], angular.toJson(requestEnvironments));
          return appStatuses;
        };

        var colors = {
          'DEPLOYED': '#398439',
          'UPDATED': '#398439',
          'UNDEPLOYED': '#D8D8D8',
          'UNKNOWN': '#505050',
          'WARNING': '#DE9600',
          'FAILURE': '#C51919',
          'DEPLOYMENT_IN_PROGRESS': '#2C80D3',
          'UNDEPLOYMENT_IN_PROGRESS': '#D0ADAD'
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
                  segment.color = colors[envDTO.status];
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

        $scope.searchService = searchServiceFactory('rest/latest/applications/search', false, $scope, 14);
        $scope.searchService.search();

        $scope.onSearchCompleted = function (searchResult) {
          $scope.data = searchResult.data;
          updateApplicationStatuses(searchResult.data);
        };

        $scope.openApplication = function (applicationId) {
          $state.go('applications.detail.info', {
            id: applicationId
          });
        };

        $scope.openDeploymentPage = function (application, environmentId) {
          $state.go('applications.detail.deployment', {
            id: application.id,
            openOnEnvironment: environmentId
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
      }
    ]); // controller
}); // define
