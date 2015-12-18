define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  var d3Tip = require('d3-tip');
  var d3 = require('d3');
  window.d3 = d3;
  var d3pie = require('d3-pie');

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
      var searchTopologyTemplateResource = $resource('rest/templates/topology/search', {}, {
        'search': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var searchTopologyTemplateVersionResource = $resource('rest/templates/:topologyTemplateId/versions/search', {}, {
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
    ['$scope', '$modal', '$state', 'authService', 'applicationServices', '$translate', 'toaster', 'searchServiceFactory',
    function($scope, $modal, $state, authService, applicationServices, $translate, toaster, searchServiceFactory) {
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

      var drawPieChart = function(appName, data) {

        if (data.length > 0) {
          var tip = d3Tip().attr('class', 'd3-tip').html(function(node) {
            return node.data.name;
          });

          var pie = new d3pie('pieChart-' + appName, {
            'size': {
              'canvasWidth': 60,
              'canvasHeight': 60
            },
            'data': {
              'sortOrder': 'label-asc',
              'content': data
            },
            'labels': {
              'outer': {
                'format': 'none'
              },
              'inner': {
                'format': 'none'
              }
            },
            'effects': {
              'load': {
                'effect': 'none'
              },
              'pullOutSegmentOnClick': {
                'effect': tip.hide
              },
              'highlightSegmentOnMouseover': true,
              'highlightLuminosity': 0.10
            },
            'callbacks': {
              'onMouseoverSegment': function(data) {
                tip.show(data);
              },
              'onMouseoutSegment': tip.hide
            }
          });

          pie.svg.call(tip);
        }
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
              drawPieChart(app.name, data);
            });
          });
        }
        return applicationSearchResult;
      };

      $scope.toogleShowDeployedApplications = function() {
        $scope.onlyShowDeployedApplications = ($scope.onlyShowDeployedApplications) ? undefined : true;
        $scope.searchService.search();
      };

      $scope.searchService = searchServiceFactory('rest/applications/search', false, $scope, 14);
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

      $scope.openDeploymentPage = function(applicationId) {
        $scope.openApplication(applicationId);
        $state.go('applications.detail.deployment', {
          id: applicationId
        });
      };

      $scope.removeApplication = function(applicationId) {
        applicationServices.remove({
          applicationId: applicationId
        }, function(response) {
          if (!response.error && response.data === true) {
            $scope.searchService.search();
          } else {
            toaster.pop('error', $translate('APPLICATIONS.ERRORS.DELETE_TITLE'), $translate('APPLICATIONS.ERRORS.DELETING_FAILED'), 4000, 'trustedHtml', null);
          }
        });
      };
    }
  ]); // controller
}); // define
