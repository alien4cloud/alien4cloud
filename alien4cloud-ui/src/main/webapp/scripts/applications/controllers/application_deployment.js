define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/applications/services/application_services');
  require('scripts/applications/services/application_environment_services');
  require('scripts/applications/services/application_version_services');
  
  require('scripts/applications/controllers/application_deployment_locations');
  require('scripts/applications/controllers/application_deployment_match');
  require('scripts/applications/controllers/application_deployment_setup');

  require('scripts/deployment/directives/display_outputs');
  require('scripts/common/filters/inputs');

  states.state('applications.detail.deployment', {
    url: '/deployment',
    templateUrl: 'views/applications/application_deployment.html',
    controller: 'ApplicationDeploymentCtrl',
    menu: {
      id: 'am.applications.detail.deployment',
      state: 'applications.detail.deployment',
      key: 'NAVAPPLICATIONS.MENU_DEPLOYMENT',
      icon: 'fa fa-cloud-upload',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer
      priority: 300
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentCtrl',
    ['$scope', 'authService', '$upload', 'applicationServices', 'toscaService', '$resource', '$http', '$translate', 'application', '$state', 'applicationEnvironmentServices', 'appEnvironments', 'toaster', '$filter', 'menu',
    function($scope, authService, $upload, applicationServices, toscaService, $resource, $http, $translate, applicationResult, $state, applicationEnvironmentServices, appEnvironments, toaster, $filter, menu) {
      var pageStateId = $state.current.name;
      $scope.menu = menu;
      
      //deployment context
      $scope.deploymentContext = {};
      $scope.deploymentTopologyDTO = {}
      
      var minimumZoneCountPerGroup = 1;

      // Initialization
      $scope.application = applicationResult.data;
      $scope.envs = appEnvironments.deployEnvironments;
      $scope.getResourceIcon = function(defaultImage, key) {
        if (_.defined($scope.topologyDTO) &&
          _.defined($scope.topologyDTO.topology) &&
          _.defined($scope.topologyDTO.topology.nodeTemplates) &&
          _.defined($scope.topologyDTO.topology.nodeTemplates[key])) {
          var tags = $scope.topologyDTO.nodeTypes[$scope.topologyDTO.topology.nodeTemplates[key].type].tags;
          if (_.defined(tags)) {
            var icon = toscaService.getIcon(tags);
            return 'img?id=' + (_.defined(icon) ? icon : defaultImage) + '&quality=QUALITY_64';
          } else {
            return null;
          }
        } else {
          return null;
        }
      };
      
      
      // Retrieval and validation of the topology associated with the deployment.
      var checkTopology = function (){

        $scope.isTopologyValid($scope.topologyId, $scope.selectedEnvironment.id).$promise.then(function(validTopologyResult) {
          $scope.validTopologyDTO = validTopologyResult.data;
          prepareTasksAndWarnings($scope.validTopologyDTO);
        });

        var processTopologyInfoResult = $scope.processTopologyInformations($scope.topologyId);

        // when the selected environment is deployed => refresh output properties
        processTopologyInfoResult.$promise.then(function() {
          if ($scope.selectedEnvironment.status === 'DEPLOYED') {
            $scope.refreshInstancesStatuses($scope.application.id, $scope.selectedEnvironment.id, pageStateId);
          }
        });

      }
      
      //register the checking topo function for others states to use it
      $scope.checkTopology = checkTopology;

      // set the environment to the given one and forward to locations screen.
      var setEnvironmentAndForwardToLocations = function(environment){
        $scope.selectedEnvironment = environment;
        $scope.setTopologyId($scope.application.id, $scope.selectedEnvironment.id, checkTopology).$promise.then(function() {
//          console.log($scope.topologyId);
          $state.go('applications.detail.deployment.locations');
        });
      }
      setEnvironmentAndForwardToLocations($scope.envs[0]); // default env

      // Just group tasks / warnings by category for the display
      function prepareTasksAndWarnings(validTopologyDTO) {
        // currently prepare warnings
        if (_.defined(validTopologyDTO.warningList)) {
          var preparedWarningList = {};
          validTopologyDTO.warningList.forEach(function(task) {
            if (!preparedWarningList.hasOwnProperty(task.code)) {
              preparedWarningList[task.code] = [];
            }
            preparedWarningList[task.code].push(task);
          });
          // replace the default warning list
          validTopologyDTO.warningList = preparedWarningList;
        }
      }

      // Change the selected environment (set only if required).
      var changeEnvironment = function(newEnvironment) {
        if (_.defined(newEnvironment) && newEnvironment.id !== $scope.selectedEnvironment.id) {
          setEnvironmentAndForwardToLocations(newEnvironment);
        }
      };

      
      $scope.showTodoList = function() {
        return !$scope.validTopologyDTO.valid && $scope.isManager;
      };

      $scope.showWarningList = function() {
        return angular.isObject($scope.validTopologyDTO.warningList) && Object.keys($scope.validTopologyDTO.warningList).length > 0;
      };

      
      // Map functions that should be available from scope.
      $scope.changeEnvironment = changeEnvironment;

      // Application rights
      $scope.isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
      $scope.isDevops = authService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
      // Application environment rights
      $scope.isDeployer = authService.hasResourceRole($scope.selectedEnvironment, 'DEPLOYMENT_MANAGER');
      $scope.isUser = authService.hasResourceRole($scope.selectedEnvironment, 'APPLICATION_USER');

      $scope.finalOutputAttributesValue = $scope.outputAttributesValue;
      $scope.validTopologyDTO = false;

      $scope.isAllowedDeployment = function() {
        return $scope.isDeployer || $scope.isManager;
      };

      // if the status or the environment changes we must update the event registration.
      $scope.$watch(function(scope) {
        if (_.defined(scope.selectedEnvironment)) {
          return scope.selectedEnvironment.id + '__' + scope.selectedEnvironment.status;
        }
        return 'UNDEPLOYED';
      }, function(newValue) {
        var undeployedValue = $scope.selectedEnvironment.id + '__UNDEPLOYED';
        // no registration for this environement -> register if not undeployed!
        if (newValue === undeployedValue) {
          // if status the application is not undeployed we should register for events.
          $scope.stopEvent();
        } else {
          $scope.stopEvent();
          $scope.setTopologyId($scope.application.id, $scope.selectedEnvironment.id, checkTopology).$promise.then(function(result) {
            $scope.processTopologyInformations(result.data).$promise.then(function() {
              $scope.refreshInstancesStatuses($scope.application.id, $scope.selectedEnvironment.id, pageStateId);
            });
          });

        }
      });

      // when scope change, stop current event listener
      $scope.$on('$destroy', function() {
        $scope.stopEvent();
      });

    }
  ]); //controller
}); //Define
