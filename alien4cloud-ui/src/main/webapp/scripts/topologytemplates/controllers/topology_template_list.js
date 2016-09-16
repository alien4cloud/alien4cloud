// topology templates list is the entry point for browsing and managing global topologies in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');
  var _ = require('lodash');

  // register components root state
  states.state('topologycatalog', {
    url: '/topologycatalog',
    template: '<ui-view/>',
    menu: {
      id: 'menu.topologycatalog',
      state: 'topologycatalog',
      key: 'NAVBAR.MENU_TOPOLOGY_TEMPLATE',
      icon: 'fa fa-sitemap',
      priority: 20,
      roles: ['ARCHITECT']
    }
  });
  states.state('topologycatalog.list', {
    url: '/list',
    templateUrl: 'views/topologytemplates/topology_template_list.html',
    controller: 'TopologyTemplateListCtrl'
  });
  states.forward('topologycatalog', 'topologycatalog.list');

  // states.state('topologycatalog.csar', {
  //   url: '/csar',
  //   templateUrl: 'views/layout/vertical_menu_layout.html',
  //   controller: 'LayoutCtrl',
  // });

  var registerService = require('scripts/topology/editor_register_service');
  registerService('topologycatalog.csar');

  states.state('topologycatalog.csar', {
    url: '/topology/:archiveName/edit/:archiveVersion',
    templateUrl: 'views/topology/topology_editor_layout.html',
    controller: 'TopologyEditorCtrl',
    resolve: {
      archiveVersions: ['$alresource', '$stateParams',
        function($alresource, $stateParams) {
          // Get all versions of the archive.
          return $alresource('rest/latest/catalog/topologies/:archiveName/versions')
            .get({archiveName: $stateParams.archiveName}).$promise;
        }
      ],
      context: ['$stateParams', function ($stateParams) {
        var context = { topologyId: undefined };
        if (!_.isEmpty($stateParams.archiveVersion)) {
          context.versionName = $stateParams.archiveVersion;
        }
        return context;
      }]
    }
  });

  var NewTopologyTemplateCtrl = ['$scope', '$modalInstance', 'applicationVersionServices',
    function($scope, $modalInstance, applicationVersionServices) {
      $scope.topologytemplate = {};
      $scope.topologytemplate.version = '0.1.0-SNAPSHOT';
      $scope.versionPattern = applicationVersionServices.pattern;
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
    ['$scope', '$modal', '$resource', '$state', 'authService', '$alresource',
    function($scope, $modal, $resource, $state, authService, $alresource) {
      $scope.onSearch = function (searchConfig) {
        $scope.searchConfig = searchConfig;
      };
      $scope.openCsar = function(csarId, event) {
        if (_.defined(event)) {
          event.stopPropagation();
        }
        $state.go('components.csars.csardetail', { csarId: csarId });
      };
      $scope.openTopology = function(archiveName, archiveVersion) {
        $state.go('topologycatalog.csar', { archiveName: archiveName, archiveVersion: archiveVersion });
      };
      var fetchVersionsResource = $alresource('rest/latest/catalog/topologies/:archiveName/versions');
      $scope.fetchVersions = function(topology) {
        if(_.defined(topology.allVersions)) {
          return;
        }
        fetchVersionsResource.get({archiveName: topology.archiveName},function(result) {
          if(_.defined(result.error)) {
            console.error('Encountered error while fetching element versions', topology.archiveName, result.error);
          } else {
            topology.allVersions = result.data;
          }
        });
      };

      // API REST Definition
      var createTopologyTemplateResource = $resource('/rest/latest/catalog/topologies/template', {}, {
        'create': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      $scope.openNewTopologyTemplate = function() {
        var modalInstance = $modal.open({
          templateUrl: 'views/topologytemplates/topology_template_new.html',
          controller: NewTopologyTemplateCtrl
        });

        modalInstance.result.then(function(topologyTemplate) {
          // create a new topologyTemplate from the given name, version and description.
          createTopologyTemplateResource.create([], angular.toJson(topologyTemplate), function(response) {
            // Response contains topology id
            if (response.data !== '') {
              $scope.openTopologyTemplate(response.data);
            }
          });
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
