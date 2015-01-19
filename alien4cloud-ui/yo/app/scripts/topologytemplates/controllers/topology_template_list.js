'use strict';

var NewTopologyTemplateCtrl = ['$scope', '$modalInstance',
  function($scope, $modalInstance) {
    $scope.topologytemplate = {};
    $scope.create = function(valid) {
      if (valid) {
        $modalInstance.close($scope.topologytemplate);
      }
    };
    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
  }
];

angular.module('alienUiApp').controller('TopologyTemplateListCtrl', ['$scope', '$modal', '$resource', '$state', 'alienAuthService',
  function($scope, $modal, $resource, $state, alienAuthService) {

    // API REST Definition
    var createTopologyTemplateResource = $resource('rest/templates/topology', {}, {
      'create': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    var topologyTemplateResource = $resource('rest/templates/topology/:topologyTemplateId', {}, {
      'get': {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      },
      'remove': {
        method: 'DELETE'
      }
    });

    var searchTopologyTemplateResource = $resource('rest/templates/topology/search', {}, {
      'search': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });

    $scope.openNewTopologyTemplate = function() {
      var modalInstance = $modal.open({
        templateUrl: 'newTopologyTemplate.html',
        controller: NewTopologyTemplateCtrl
      });

      modalInstance.result.then(function(topologyTemplate) {
        // create a new topologyTemplate from the given name and description.
        createTopologyTemplateResource.create([], angular.toJson(topologyTemplate), function(response) {
          // Response contains topology template id
          if (response.data !== '') {
            $scope.openTopologyTemplate(response.data);
          }
          $scope.search();
        });
      });
    };

    $scope.search = function() {
      var searchRequestObject = {
        'query': $scope.query,
        'from': 0,
        'size': 50
      };
      $scope.searchResult = searchTopologyTemplateResource.search([], angular.toJson(searchRequestObject));
    };

    $scope.search();

    $scope.openTopologyTemplate = function(topologyTemplateId) {
      $state.go('topologytemplates.detail.topology', {
        id: topologyTemplateId
      });
    };

    $scope.deleteTopologyTemplate = function(topologyTemplateId) {
      topologyTemplateResource.remove({
        topologyTemplateId: topologyTemplateId
      }, function(response) {
        // Response contains topology template id
        if (response.data !== '') {
          $scope.search();
        }
      });
    };

    $scope.isArchitect = alienAuthService.hasRole('ARCHITECT');
  }

]);
