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

    var applicationResource = $resource('rest/applications', {}, {
      'create': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    $scope.openNewApp = function() {
      var modalInstance = $modal.open({
        templateUrl: 'newApplication.html',
        controller: NewApplicationCtrl
      });

      modalInstance.result.then(function(application) {
        // create a new application from the given name and description.
        applicationResource.create([], angular.toJson(application), function(successResponse) {
          $scope.openApplication(successResponse.data);
        });
      });
    };

    // API REST Definition
    var searchResource = $resource('rest/applications/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

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
        // getting statuses for all applications
        var statuses = getApplicationStatuses(applicationSearchResult.data.data);
        // enhancing applications list with statuses
        Object.keys(applicationSearchResult.data.data).forEach(function(key) {
          var app = applicationSearchResult.data.data[key];
          statuses.$promise.then(function(statuses) {
            app.status = statuses.data[app.id];
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
      var searchResult = searchResource.search([], angular.toJson(searchRequestObject));

      // when apps search result is ready, update apps statuses
      searchResult.$promise.then(function(applisationListResult) {
        updateApplicationStatuses(applisationListResult);
      });
      $scope.searchResult = searchResult;
    };
    $scope.search();

    $scope.openApplication = function(applicationId) {
      $state.go('applications.detail.info', { id: applicationId });
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
