'use strict';

var NewApplicationCtrl = ['$scope', '$modalInstance', '$resource',
  function($scope, $modalInstance, $resource) {
    $scope.app = {};
    $scope.create = function(valid, templateId) {
      if (valid) {
        if (!angular.isUndefined(templateId)) {
          // topologyId linked to the topology template
          $scope.app.topologyId = templateId;
        }
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

    // First template load
    $scope.loadTopologyTemplates();
  }
];

angular.module('alienUiApp').controller('ApplicationListCtrl', ['$scope', '$modal', '$resource', '$state', 'alienAuthService', 'applicationServices', '$translate', 'toaster',
  function($scope, $modal, $resource, $state, alienAuthService, applicationServices, $translate, toaster) {
    $scope.isManager = alienAuthService.hasRole('APPLICATIONS_MANAGER');

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

    var countStatus = function(app, statuses) {
      app.sumByStatus = {'DEPLOYED' :0, 'UNDEPLOYED' :0, 'DEPLOYMENT_IN_PROGRESS' :0, 'UNDEPLOYMENT_IN_PROGRESS' :0, 'WARNING' :0, 'FAILURE' :0, 'DEPLOY' :0, 'UNDEPLOY' :0};
      for (var key in statuses) {
        app.sumByStatus[statuses[key]] ++;
      }
    }

    // Update applications statuses
    var getApplicationStatuses = function(applications) {
      var requestAppStatuses = [];
      Object.keys(applications).forEach(function(key) {
        requestAppStatuses.push(applications[key].id);
      });
      var appStatuses = applicationServices.applicationStatus.statuses([], angular.toJson(requestAppStatuses));
      return appStatuses;
    };

    var updateApplicationStatuses = function(applicationSearchResult) {
      if (!angular.isUndefined(applicationSearchResult)) {
        var statuses = getApplicationStatuses(applicationSearchResult.data.data);
        Object.keys(applicationSearchResult.data.data).forEach(function(key) {
          var app = applicationSearchResult.data.data[key];
          statuses.$promise.then(function(statuses) {

            var colors = {'DEPLOYED': '#3ADF00', 'UNDEPLOYED': '#D8D8D8'};
            var datas = [];
            var segment = {};
            var tmpArray = statuses.data[app.id];
            for (var key in tmpArray) {
              segment['label'] = tmpArray[key];
              segment['color'] = colors[tmpArray[key]];
              segment['value'] = 1;
              datas.push(segment);
            }

            var pie = new d3pie("pieChart-" + app.name, {
              "header": {
                "title": {
                  "fontSize": 24,
                  "font": "open sans"
                },
                "subtitle": {
                  "color": "#999999",
                  "fontSize": 12,
                  "font": "open sans"
                },
                "titleSubtitlePadding": 9
              },
              "footer": {
                "color": "#999999",
                "fontSize": 10,
                "font": "open sans",
                "location": "bottom-left"
              },
              "size": {
                "canvasWidth": 100,
                "canvasHeight": 100
              },
              "data": {
                "sortOrder": "value-desc",
                "content": datas
              },
              "labels": {
                "outer": {
                  "format": "none",
                  "pieDistance": 32
                },
                "inner": {
                  "format": "none",
                  "hideWhenLessThanPercentage": 3
                },
                "mainLabel": {
                  "fontSize": 11
                },
                "percentage": {
                  "color": "#ffffff",
                  "decimalPlaces": 0
                },
                "value": {
                  "color": "#adadad",
                  "fontSize": 11
                },
                "lines": {
                  "enabled": true
                },
              },
              "effects": {
                "load": {
                  "effect": "none"
                },
                "pullOutSegmentOnClick": {
                  "effect": "none"
                },
                "highlightSegmentOnMouseover": true,
                "highlightLuminosity": 0.99
              },
            });

          });
        });
      }
      return applicationSearchResult;
    };

    $scope.search = function() {
      var searchRequestObject = {
        'query': $scope.query,
        'from': 0,
        'size': 50
      };
      var searchResult = applicationServices.search([], angular.toJson(searchRequestObject));

      // when apps search result is ready, update apps statuses
      searchResult.$promise.then(function(applisationListResult) {
        updateApplicationStatuses(applisationListResult);
      });
      $scope.searchResult = searchResult;
    };
    $scope.search();

    $scope.openApplication = function(applicationId) {
      $state.go('applications.detail.info', {
        id: applicationId
      });
    };

    $scope.removeApplication = function(applicationId) {
      applicationServices.remove({
        applicationId: applicationId
      }, function(response) {
        if (!response.error && response.data === true) {
          $scope.search();
        } else {
          // toaster message
          toaster.pop('error', $translate('APPLICATIONS.ERRORS.DELETE_TITLE'), $translate('APPLICATIONS.ERRORS.DELETING_FAILED'), 4000, 'trustedHtml', null);
        }
      });
    };
  }
]);
