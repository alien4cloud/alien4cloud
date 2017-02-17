define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  require('scripts/applications/services/runtime_event_service');
  require('scripts/applications/controllers/runtime_topology');
  require('scripts/applications/controllers/runtime_workflows');

  states.state('applications.detail.runtime', {
    url: '/runtime',
    templateUrl: 'views/topology/topology_runtime_layout.html',
    controller: 'a4cRuntimeCtrl',
    resolve: {
      resource: ['application',
        function(application) { // to apply resource roles on the menu element.
          return application.data;
        }
      ],
    },
    menu: {
      id: 'am.applications.detail.runtime',
      state: 'applications.detail.runtime',
      key: 'NAVAPPLICATIONS.MENU_RUNTIME',
      icon: 'fa fa-cogs',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer
      priority: 400
    }
  });

  // This controller manage runtime view topology fetch and events registrations.
  modules.get('a4c-applications').controller('a4cRuntimeCtrl',
    ['$scope', '$state', 'menu', 'layoutService',
    'deploymentServices', 'orchestratorService', 'locationService',
    'appEnvironments', 'topologyJsonProcessor', 'a4cRuntimeEventService',
    function($scope, $state, menu, layoutService,
      deploymentServices, orchestratorService, locationService,
      appEnvironments, topologyJsonProcessor, a4cRuntimeEventService) {
      // side-menu management
      layoutService.process(menu);
      $scope.menu = menu;

      // used for event registrations
      var pageStateId = $state.current.name;
      appEnvironments.selectDeployed();
      $scope.selectedEnvironment = appEnvironments.selected;
      $scope.runtimeEnvironments = appEnvironments.deployEnvironments;

      // register for deployment events.
      a4cRuntimeEventService($scope, $scope.application.id, pageStateId);

      // Method triggered when the environment changes, allow to update the orchestrator and locations details.
      function refreshOrchestratorInfo() {
        var newLocationId = $scope.topology.topology.locationGroups._A4C_ALL.policies[0].locationId;
        if(_.defined($scope.location) && $scope.location.id === newLocationId) {
          return; // current informations are already valid.
        }
        locationService.get({
          orchestratorId: $scope.topology.topology.orchestratorId,
          locationId: $scope.topology.topology.locationGroups._A4C_ALL.policies[0].locationId
        }, function(response) {
          $scope.location = response.data.location;
        });
        orchestratorService.get({
          orchestratorId: $scope.topology.topology.orchestratorId
        }, function(response) {
          $scope.orchestrator = response.data;
        });
      }

      function loadTopologyRuntime() {
        delete $scope.topology;
        $scope.$broadcast('a4cRuntimeTopologyLoading');
        deploymentServices.runtime.getTopology({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, function(successResult) { // get the topology
          $scope.topology = successResult.data;
          topologyJsonProcessor.process($scope.topology);
          // dispatch an event through the scope
          $scope.$broadcast('a4cRuntimeTopologyLoaded');
          refreshOrchestratorInfo();
          // refreshInstancesStatuses(); // update instance states
        });
      }

      // Load the topology based on the current environment.
      loadTopologyRuntime();

      // Environment selection
      $scope.changeEnvironment = function(selectedEnvironment) {
        appEnvironments.select(selectedEnvironment.id, function() {
          $scope.selectedEnvironment = appEnvironments.selected;
          // update the environment
          loadTopologyRuntime();
        });
      };
    }
  ]);
});
