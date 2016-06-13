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

  var NewApplicationCtrl = ['$scope', '$modalInstance', '$resource',
    function($scope, $modalInstance, $resource) {
      $scope.app = {};
      $scope.create = function(valid, templateVersionId) {
        if (valid) {
          $scope.app.topologyTemplateVersionId = templateVersionId;
          $modalInstance.close($scope.app);
        }
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };

      // TopologyTemplate handeling
      var searchTopologyTemplateResource = $resource('rest/latest/templates/topology/search', {}, {
        'search': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var searchTopologyTemplateVersionResource = $resource('rest/latest/templates/:topologyTemplateId/versions/search', {}, {
        'search': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      $scope.loadTopologyTemplates = function() {
        var searchRequestObject = {
          'query': $scope.query,
          'from': 0,
          'size': 50
        };
        searchTopologyTemplateResource.search([], angular.toJson(searchRequestObject), function(successResult) {
          $scope.templates = successResult.data.data;
        });
      };

      $scope.loadTopologyTemplateVersions = function(selectedTemplateId) {
        var searchRequestObject = {
            'query': $scope.query,
            'from': 0,
            'size': 50
          };
        searchTopologyTemplateVersionResource.search({topologyTemplateId: selectedTemplateId}, angular.toJson(searchRequestObject), function(successResult) {
          $scope.templateVersions = successResult.data.data;
        });
      };

      $scope.templateSelected = function(selectedTemplateId) {
        $scope.templateVersions = undefined;
        $scope.selectedTopologyTemplateVersion = undefined;
        if (selectedTemplateId === '') {
          $scope.selectedTopologyTemplate = undefined;
        } else {
          _.each($scope.templates, function(t) {
            if (t.id === selectedTemplateId) {
              $scope.selectedTopologyTemplate = t;
            }
          });
        }
        if ($scope.selectedTopologyTemplate) {
          $scope.loadTopologyTemplateVersions($scope.selectedTopologyTemplate.id);
        }
      };

      // First template load
      $scope.loadTopologyTemplates();
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
          templateUrl: 'newApplication.html',
          controller: NewApplicationCtrl
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
              pieChartService.render(app.name, data);
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
