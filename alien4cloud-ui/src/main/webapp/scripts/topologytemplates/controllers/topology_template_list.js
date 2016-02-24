// topology templates list is the entry point for browsing and managing global topologies in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('scripts/topologytemplates/controllers/topology_template');

  // register components root state
  states.state('topologytemplates', {
    url: '/topologytemplates',
    template: '<ui-view/>',
    menu: {
      id: 'menu.topologytemplates',
      state: 'topologytemplates',
      key: 'NAVBAR.MENU_TOPOLOGY_TEMPLATE',
      icon: 'fa fa-sitemap',
      priority: 20,
      roles: ['ARCHITECT']
    }
  });
  states.state('topologytemplates.list', {
    url: '/list',
    templateUrl: 'views/topologytemplates/topology_template_list.html',
    controller: 'TopologyTemplateListCtrl'
  });
  states.forward('topologytemplates', 'topologytemplates.list');

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

  modules.get('a4c-topology-templates', ['ui.router', 'ui.bootstrap', 'a4c-auth', 'a4c-common']).controller('TopologyTemplateListCtrl',
    ['$scope', '$modal', '$resource', '$state', 'authService', 'searchServiceFactory',
    function($scope, $modal, $resource, $state, authService, searchServiceFactory) {

      // API REST Definition
      var createTopologyTemplateResource = $resource('rest/v1/templates/topology', {}, {
        'create': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var topologyTemplateResource = $resource('rest/v1/templates/topology/:topologyTemplateId', {}, {
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

      $scope.openNewTopologyTemplate = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/topologytemplates/topology_template_new.html',
          controller: NewTopologyTemplateCtrl
        });

        modalInstance.result.then(function(topologyTemplate) {
          // create a new topologyTemplate from the given name and description.
          createTopologyTemplateResource.create([], angular.toJson(topologyTemplate), function(response) {
            // Response contains topology template id
            if (response.data !== '') {
              $scope.openTopologyTemplate(response.data);
            }
          });
        });
      };

      $scope.onSearchCompleted = function(searchResult) {
        $scope.data = searchResult.data;
      };

      $scope.searchService = searchServiceFactory('rest/v1/templates/topology/search', false, $scope, 12);
      $scope.searchService.search();

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
            $scope.searchService.search();
          }
        });
      };

      var isArchitectPromise = authService.hasRole('ARCHITECT');
      if(_.isBoolean(isArchitectPromise)) {
        $scope.isArchitect = isArchitectPromise;
      } else {
        $scope.isArchitect = false;
        isArchitectPromise.then(function(isArchitect) { $scope.isArchitect = isArchitect; });
      }
    }
  ]); // controller
}); // define
