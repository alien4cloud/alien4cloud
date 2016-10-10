define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var d3 = require('d3');
  var _ = require('lodash');

  require('scripts/common/services/pie_chart_service.js');
  require('scripts/applications/services/application_services');
  require('scripts/applications/controllers/application_detail');
  require('scripts/topology/services/topology_services');

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

  var NewApplicationCtrl = ['$scope', '$modalInstance', 'topologyServices',
    function($scope, $modalInstance, topologyServices) {
      $scope.app = {};
      var autoGenArchiveName = true;
      $scope.nameChange= function() {
        if(autoGenArchiveName) {
          $scope.app.archiveName = _.capitalize(_.camelCase($scope.app.name));
        }
      };
      $scope.archiveNameChange= function() {
        autoGenArchiveName = false;
      };
      $scope.topologyNeedsRecovery = false;
      $scope.selectTemplate= function(topology) {
        $scope.app.topologyTemplateName = topology.archiveName;
        $scope.app.topologyTemplateVersion = topology.archiveVersion;
        $scope.app.topologyTemplateVersionId = topology.id;
        topologyServices.dao.get({
          topologyId: topology.id
        }, function(result) {
          if(_.defined(result.error) && result.error.code === 860){
            $scope.topologyNeedsRecovery = true;
          } else {
            $scope.topologyNeedsRecovery = false;
          }
        });
      };
      $scope.create = function(valid) {
        if (valid) {
          $modalInstance.close($scope.app);
        }
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ];

  modules.get('a4c-applications').controller('ApplicationListCtrl',
    ['$scope', '$modal', '$state', 'authService', 'applicationServices', '$translate', 'toaster', 'searchServiceFactory', 'pieChartService',
    function($scope, $modal, $state, authService, applicationServices, $translate, toaster, searchServiceFactory, pieChartService) {
      $scope.isManager = authService.hasRole('APPLICATIONS_MANAGER');
      $scope.applicationStatuses = [];
      $scope.onlyShowDeployedApplications = undefined;
      d3.selectAll('.d3-tip').remove();

      $scope.openNewApp = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/applications/application_new.html',
          controller: NewApplicationCtrl,
          windowClass: 'new-app-modal'
        });
        modalInstance.result.then(function(application) {
          // create a new application from the given name and description.
          applicationServices.create([], angular.toJson(application), function(successResponse) {
            $scope.openApplication(successResponse.data);
          });
        });
      };

      var getApplicationStatuses = function(applications) {
        var requestAppStatuses = [];
        Object.keys(applications).forEach(function(key) {
          requestAppStatuses.push(applications[key].id);
        });
        var appStatuses = applicationServices.applicationStatus.statuses([], angular.toJson(requestAppStatuses));
        return appStatuses;
      };

      var colors = {
        'DEPLOYED': '#398439',
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

      var updateApplicationStatuses = function(applicationSearchResult) {
        if (!angular.isUndefined(applicationSearchResult)) {
          var statuses = getApplicationStatuses(applicationSearchResult.data);
          Object.keys(applicationSearchResult.data).forEach(function(key) {
            var app = applicationSearchResult.data[key];
            statuses.$promise.then(function(statuses) {
              var data = [];
              var tmpArray = statuses.data[app.id];
              for (var key in tmpArray) {
                var segment = {};
                segment.label = tmpArray[key].environmentStatus;
                segment.color = colors[tmpArray[key].environmentStatus];
                segment.indexToOrder = priorityToOrder[tmpArray[key].environmentStatus];
                segment.value = 1;
                segment.id = key;
                segment.name = tmpArray[key].environmentName;

                // Initial the counter of number deployed environment by applications to sort
                if (!_.isNumber(app.countDeployedEnvironment)) {
                  app.countDeployedEnvironment = 0;
                  applicationSearchResult.data[key] = app;
                }

                if (segment.label === 'DEPLOYED') {
                  app.countDeployedEnvironment++;
                }

                // Here we manage a filter to display the deployed applications
                if ($scope.onlyShowDeployedApplications && segment.label === 'DEPLOYED') {
                  app.isDeployed = true;
                  data.push(segment);
                } else if (!$scope.onlyShowDeployedApplications) {
                  data.push(segment);
                }

                applicationSearchResult.data[key] = app;
              }
              $scope.applicationStatuses[app.name] = data;
              pieChartService.render(app.id, data);
            });
          });
        }
        return applicationSearchResult;
      };

      $scope.toogleShowDeployedApplications = function() {
        $scope.onlyShowDeployedApplications = ($scope.onlyShowDeployedApplications) ? undefined : true;
        $scope.searchService.search();
      };

      $scope.searchService = searchServiceFactory('rest/latest/applications/search', false, $scope, 14);
      $scope.searchService.search();

      $scope.onSearchCompleted = function(searchResult) {
        $scope.data = searchResult.data;
        updateApplicationStatuses(searchResult.data);
      };

      $scope.openApplication = function(applicationId) {
        $state.go('applications.detail.info', {
          id: applicationId
        });
      };

      $scope.openDeploymentPage = function(applicationId, environmentId) {
        $scope.openApplication(applicationId);
        $state.go('applications.detail.deployment', {
          id: applicationId,
          openOnEnvironment: environmentId
        });
      };

      $scope.removeApplication = function(applicationId) {
        applicationServices.remove({
          applicationId: applicationId
        }, function(response) {
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
