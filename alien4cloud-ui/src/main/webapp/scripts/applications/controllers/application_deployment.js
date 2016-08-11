define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/common/filters/inputs');

  require('scripts/applications/services/application_services');
  require('scripts/applications/services/application_environment_services');
  require('scripts/applications/services/application_version_services');
  require('scripts/applications/services/deployment_topology_processor.js');
  require('scripts/applications/services/tasks_processor.js');

  require('scripts/applications/controllers/application_deployment_locations');
  require('scripts/applications/controllers/application_deployment_match');
  require('scripts/applications/controllers/application_deployment_input');
  require('scripts/applications/controllers/application_deployment_deploy');


  require('scripts/deployment/directives/display_outputs');
  require('scripts/applications/directives/topology_errors_display');

  var globalConfTaskCodes = ['SCALABLE_CAPABILITY_INVALID', 'PROPERTIES', 'NODE_FILTER_INVALID'];

  function refreshDeploymentContext(deploymentContext, application, deploymentTopologyServices, deploymentTopologyProcessor, tasksProcessor, menus) {
    return deploymentTopologyServices.get({
      appId: application.id,
      envId: deploymentContext.selectedEnvironment.id
    }).$promise.then(function(response) {
        deploymentTopologyProcessor.process(response.data);
        deploymentContext.deploymentTopologyDTO = response.data;
        tasksProcessor.processAll(deploymentContext.deploymentTopologyDTO.validation);
        updateStepsStatuses(menus, deploymentContext.deploymentTopologyDTO.validation);
        return deploymentContext;
      });
  }

  var setNextStepMenuEnabled = function(currentStepMenu, nextStepMenu){
    nextStepMenu.disabled = currentStepMenu.disabled || (_.get(currentStepMenu, 'step.status', 'SUCCESS')!=='SUCCESS');
  };

  function buildMenuTree(menus) {
    _.each(menus, function(menu){
      if (_.definedPath(menu, 'step.nextStepId')){
        menu.nextStep = _.find(menus, function(item){
          return item.id===menu.step.nextStepId;
        });
      }
    });
  }

  function enabledOrDisableMenus(menus){
    _.each(menus, function(menu){
      if (_.defined(menu.nextStep)){
        setNextStepMenuEnabled(menu, menu.nextStep);
      }
    });
  }

  function updateStepsStatuses(menus, validationDTO){
    _.each(menus, function(menu){
      if(_.definedPath(menu, 'step.taskCodes')){
        delete menu.step.status;
        _.each(menu.step.taskCodes, function(taskCode){
          if(_.definedPath(validationDTO, 'taskList['+taskCode+']')){
            menu.step.status = 'ERROR';
            return;
          }
        });
      }
    });

    enabledOrDisableMenus(menus);
  }

  states.state('applications.detail.deployment', {
    url: '/deployment',
    resolve: {
      deploymentContext: ['application', 'appEnvironments', 'deploymentTopologyServices', 'deploymentTopologyProcessor', 'tasksProcessor', 'menu',
        function(application, appEnvironments, deploymentTopologyServices, deploymentTopologyProcessor, tasksProcessor, menu) {
          //build the menu tree
          buildMenuTree(menu);
          var deploymentContextResult = {
            selectedEnvironment: appEnvironments.selected
          };
          return refreshDeploymentContext(deploymentContextResult, application.data, deploymentTopologyServices, deploymentTopologyProcessor, tasksProcessor, menu);
        }
      ]
    },
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
    ['$scope', 'authService', 'application', '$state', 'appEnvironments', 'menu', 'deploymentContext', 'deploymentTopologyServices', 'deploymentTopologyProcessor', 'applicationServices', 'tasksProcessor',
      function($scope, authService, applicationResult, $state, appEnvironments, menu, deploymentContext, deploymentTopologyServices, deploymentTopologyProcessor, applicationServices, tasksProcessor) {
        $scope.deploymentContext = deploymentContext;
        var pageStateId = $state.current.name;
        $scope.menu = menu;

        // Initialization
        $scope.application = applicationResult.data;
        $scope.envs = appEnvironments.deployEnvironments;

        $scope.undeploy = function() {
          $scope.isUnDeploying = true;
          applicationServices.deployment.undeploy({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.deploymentContext.selectedEnvironment.id
          }, function() {
            $scope.deploymentContext.selectedEnvironment.status = 'UNDEPLOYMENT_IN_PROGRESS';
            $scope.isUnDeploying = false;
            $scope.stopEvent();
          }, function() {
            $scope.isUnDeploying = false;
          });
        };

        // Retrieval and validation of the topology associated with the deployment.
        var checkTopology = function() {

          $scope.isTopologyValid($scope.topologyId, $scope.deploymentContext.selectedEnvironment.id).$promise.then(function(validTopologyResult) {
            $scope.validTopologyDTO = validTopologyResult.data;
            tasksProcessor.processAll($scope.validTopologyDTO);
          });

          var processTopologyInfoResult = $scope.processTopologyInformations($scope.topologyId);

          // when the selected environment is deployed => refresh output properties
          processTopologyInfoResult.$promise.then(function() {
            if ($scope.deploymentContext.selectedEnvironment.status === 'DEPLOYED') {
              $scope.refreshInstancesStatuses($scope.application.id, $scope.deploymentContext.selectedEnvironment.id, pageStateId);
            }
          });

        };

        //register the checking topo function for others states to use it
        $scope.checkTopology = checkTopology;

        function doGoToNextInvalidStep(){
          //first step is locations
          var stepToGo = _.find($scope.menu, function(menu){
            return menu.id==='am.applications.detail.deployment.locations';
          });

          //look for the first invalid step, or the last one if all are valid
          while(stepToGo.nextStep){
            if(_.get(stepToGo, 'step.status', 'SUCCESS') !== 'SUCCESS'){
              break;
            }
            stepToGo = stepToGo.nextStep;
          }

          //go to the found step
          $state.go(stepToGo.state);
        }

        function goToNextInvalidStep() {
          //refresh initial topo validation first
          $scope.setTopologyId($scope.application.id, $scope.deploymentContext.selectedEnvironment.id, checkTopology).$promise.then(function() {
            //then refresh deployment context
            refreshDeploymentContext($scope.deploymentContext, $scope.application, deploymentTopologyServices, deploymentTopologyProcessor, tasksProcessor, menu).then(function() {
              //finally, go to the next invalid step
              doGoToNextInvalidStep();
            });
          });
        }

        $scope.onEnvironmentChange = function() {
          // update the global environment
          appEnvironments.select($scope.deploymentContext.selectedEnvironment.id, goToNextInvalidStep);
        };

        if(_.defined($state.params.openOnEnvironment) && appEnvironments.selected.id !== $state.params.openOnEnvironment){
          appEnvironments.select($state.params.openOnEnvironment, function(){
            $scope.deploymentContext.selectedEnvironment = appEnvironments.selected;
            goToNextInvalidStep();
          });
        }else{
          goToNextInvalidStep(); // immediately go to the next invalid tab
        }

        $scope.showTodoList = function() {
          return !$scope.validTopologyDTO.valid && $scope.isManager;
        };

        $scope.showWarningList = function() {
          return angular.isObject($scope.validTopologyDTO.warningList) && Object.keys($scope.validTopologyDTO.warningList).length > 0;
        };

        $scope.showConfgurationsErrors = function() {
          var show = false;
          if( _.definedPath($scope.deploymentContext, 'deploymentTopologyDTO.validation.taskList')){
            _.each(globalConfTaskCodes, function(taskCode){
              if (_.defined($scope.deploymentContext.deploymentTopologyDTO.validation.taskList[taskCode])){
                show= true;
                return;
              }
            });
          }
          return show;
        };

        // Map functions that should be available from scope.

        // Application rights
        $scope.isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
        $scope.isDevops = authService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
        // Application environment rights
        $scope.isDeployer = authService.hasResourceRole($scope.deploymentContext.selectedEnvironment, 'DEPLOYMENT_MANAGER');
        $scope.isUser = authService.hasResourceRole($scope.deploymentContext.selectedEnvironment, 'APPLICATION_USER');

        $scope.finalOutputAttributesValue = $scope.outputAttributesValue;
        $scope.validTopologyDTO = false;

        $scope.isAllowedDeployment = function() {
          return $scope.isDeployer || $scope.isManager;
        };

        // if the status or the environment changes we must update the event registration.
        $scope.$watch(function() {
          if (_.defined($scope.deploymentContext.selectedEnvironment)) {
            return $scope.deploymentContext.selectedEnvironment.id + '__' + $scope.deploymentContext.selectedEnvironment.status;
          }
          return 'UNDEPLOYED';
        }, function(newValue) {
          var undeployedValue = $scope.deploymentContext.selectedEnvironment.id + '__UNDEPLOYED';
          // no registration for this environement -> register if not undeployed!
          if (newValue === undeployedValue) {
            // if status the application is not undeployed we should register for events.
            $scope.stopEvent();
          } else {
            $scope.stopEvent();
            $scope.setTopologyId($scope.application.id, $scope.deploymentContext.selectedEnvironment.id, checkTopology).$promise.then(function(result) {
              $scope.processTopologyInformations(result.data).$promise.then(function() {
                $scope.refreshInstancesStatuses($scope.application.id, $scope.deploymentContext.selectedEnvironment.id, pageStateId);
              });
            });
          }
        });

        //proceed the deploymentTopologyDTO
        $scope.updateScopeDeploymentTopologyDTO = function(deploymentTopologyDTO){
          if(_.undefined(deploymentTopologyDTO)){
            return;
          }
          deploymentTopologyProcessor.process(deploymentTopologyDTO);
          tasksProcessor.processAll(deploymentTopologyDTO.validation);
          $scope.deploymentContext.deploymentTopologyDTO = deploymentTopologyDTO;
          updateStepsStatuses($scope.menu, $scope.deploymentContext.deploymentTopologyDTO.validation);
        };

        //show or not the status icon
        $scope.showStatusIcon = function(menuItem){
          if(menuItem.disabled || _.get(menuItem, 'step.status','SUCCESS')==='SUCCESS'){
            return false;
          }
          return true;
        };

        // when scope change, stop current event listener
        $scope.$on('$destroy', function() {
          $scope.stopEvent();
        });

      }
    ]); //controller
}); //Define
