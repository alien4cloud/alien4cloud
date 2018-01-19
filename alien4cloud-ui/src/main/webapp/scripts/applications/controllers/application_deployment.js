define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');
  var alienUtils = require('scripts/utils/alien_utils');

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

  require('scripts/deployment/directives/display_inputs');
  require('scripts/deployment/directives/display_outputs');
  require('scripts/applications/directives/topology_errors_display');

  var globalConfTaskCodes = ['SCALABLE_CAPABILITY_INVALID', 'PROPERTIES', 'NODE_FILTER_INVALID', 'ARTIFACT_INVALID', 'INPUT_ARTIFACT_INVALID'];

  var enableOrDisableNextStepMenu = function(currentStepMenu, nextStepMenu){
    nextStepMenu.disabled = currentStepMenu.disabled || (_.get(currentStepMenu, 'step.status', 'SUCCESS')!=='SUCCESS');
  };

  function enabledOrDisableMenus(menus) {
    _.each(menus, function(menu){
      //if there is a nextStep, then compute the status of this menu, otherwise it is enabled by default
      if (_.defined(menu.nextStep)){
        enableOrDisableNextStepMenu(menu, menu.nextStep);
      }
    });
  }

  function updateStepsStatuses(menus, validationDTO) {
    //set the status of each menu, based on the defined taskCodes and their presence in the validationDTO
    _.each(menus, function(menu) {
      if(_.definedPath(menu, 'step.taskCodes')) {
        delete menu.step.status;
        _.each(menu.step.taskCodes, function(taskCode) {
          if(_.definedPath(validationDTO, 'taskList['+taskCode+']')){
            menu.step.status = 'ERROR';
            return;
          }
        });
      }
    });
    //then, set a menu enabled / disabled based on the computed status of its previous step
    enabledOrDisableMenus(menus);
  }

  function refreshDeploymentContext(deploymentContext, application, deploymentTopologyServices, deploymentTopologyProcessor, tasksProcessor, menus) {
    // console.log('Refresh deployment context');
    return deploymentTopologyServices.get({
      appId: application.id,
      envId: deploymentContext.selectedEnvironment.id
    }).$promise.then(function(response) {
        // console.log('Got response', response);
        deploymentTopologyProcessor.process(response.data);
        // console.log('processed', response);
        deploymentContext.deploymentTopologyDTO = response.data;
        tasksProcessor.processAll(deploymentContext.deploymentTopologyDTO.validation);
        // console.log('task processed', deploymentContext.deploymentTopologyDTO.validation);
        // console.log('process step statuses');
        updateStepsStatuses(menus, deploymentContext.deploymentTopologyDTO.validation);
        return deploymentContext;
      });
  }

  function buildMenuTree(menus) {
    menus = _.sortBy(menus, 'priority');
    for(var i=0; i<menus.length-1; i++){
      menus[i].nextStep = menus[i+1];
    }
  }

  states.state('applications.detail.deployment', {
    url: '/deployment',
    resolve: {
      deploymentContext: ['application', 'appEnvironments', 'deploymentTopologyServices', 'deploymentTopologyProcessor', 'tasksProcessor', 'menu',
        function(application, appEnvironments, deploymentTopologyServices, deploymentTopologyProcessor, tasksProcessor, menu) {
          //build the menu tree
          buildMenuTree(menu);
          if(appEnvironments.deployEnvironments.indexOf(appEnvironments.selected) < 0) {
            // If the selected environment is not the one that the user has deployer role then select the first one with this role enabled
            appEnvironments.select(appEnvironments.deployEnvironments[0].id);
          }
          var deploymentContextResult = {
            selectedEnvironment: appEnvironments.selected
          };
          return deploymentContextResult;
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
    ['$scope', '$uibModal', 'authService', 'application', '$state', 'appEnvironments', 'menu', 'deploymentContext', 'deploymentTopologyServices', 'deploymentTopologyProcessor', 'applicationServices', 'tasksProcessor',
      function($scope, $uibModal, authService, applicationResult, $state, appEnvironments, menu, deploymentContext, deploymentTopologyServices, deploymentTopologyProcessor, applicationServices, tasksProcessor) {
        $scope.validTopologyDTOLoaded = false;
        $scope.deploymentContext = deploymentContext;
        var pageStateId = $state.current.name;
        $scope.menu = menu;
        $scope.fromStatusToCssClasses = alienUtils.fromDeploymentStatusToCssClasses;

        // Initialization
        $scope.application = applicationResult.data;
        $scope.envs = appEnvironments.deployEnvironments;

        //////////////////////////////////////
        ///  CONFIRMATION BEFORE UNDEPLOYMENT
        ///
        var UndeployConfirmationModalCtrl = ['$scope', '$uibModalInstance', '$translate', 'applicationName', 'topologyDTO', 'environment',
          function($scope, $uibModalInstance, $translate, applicationName, topologyDTO, environment) {
            $scope.deployedVersion = topologyDTO.topology.archiveVersion;
            $scope.locationResources = {};
            _.each(_.keys(topologyDTO.topology.substitutedNodes), function(name){
              $scope.locationResources[name] = topologyDTO.topology.nodeTemplates[name];
            });
            $scope.application = applicationName;
            $scope.environment = environment;

            $scope.undeploy = function () {
              $uibModalInstance.close();
            };
            $scope.close = function () {
              $uibModalInstance.dismiss();
            };
          }
        ];

        function doUndeploy() {
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
        }

        $scope.undeploy = function() {
          var modalInstance = $uibModal.open({
            templateUrl: 'views/applications/undeploy_confirm_modal.html',
            controller: UndeployConfirmationModalCtrl,
            resolve: {
              applicationName: function() {
                return $scope.application.name;
              },
              topologyDTO: function() {
                return $scope.deployedContext.dto;
              },
              environment: function() {
                return $scope.deploymentContext.selectedEnvironment;
              }
            },
            size: 'lg'
          });

          modalInstance.result.then(function() {
            doUndeploy();
          });
        };

        // Retrieval and validation of the topology associated with the deployment.
        var checkTopology = function() {
          $scope.isTopologyValid($scope.topologyId).$promise.then(function(validTopologyResult) {
            $scope.validTopologyDTO = validTopologyResult.data;
            tasksProcessor.processAll($scope.validTopologyDTO);
            $scope.validTopologyDTOLoaded = true;
          });

          $scope.processTopologyInformations($scope.topologyId).$promise.then(function() {
            delete $scope.deployedContext.dto ;
            if($scope.deploymentContext.selectedEnvironment.status !== 'UNDEPLOYED'){
              $scope.processDeploymentTopologyInformation().$promise.then(function() {
                $scope.refreshInstancesStatuses($scope.application.id, $scope.deploymentContext.selectedEnvironment.id, pageStateId);
              });
            }
          });
        };

        //register the checking topo function for others states to use it
        $scope.checkTopology = checkTopology;

        var goToNextInvalidStep = function(){
          //menus are sorted by priority. first step is the top one
          var stepToGo = $scope.menu[0];

          //look for the first invalid step, or the last one if all are valid
          while(stepToGo.nextStep){
            if(_.get(stepToGo, 'step.status', 'SUCCESS') !== 'SUCCESS'){
              break;
            }
            stepToGo = stepToGo.nextStep;
          }

          //go to the found step
          $state.go(stepToGo.state);
        };

        $scope.goToNextInvalidStep = goToNextInvalidStep;

        function refreshDeploymentSetupStatus() {
          //refresh initial topo validation first
          $scope.setTopologyIdFromEnvironment($scope.deploymentContext.selectedEnvironment);
          checkTopology();
          //then refresh deployment context
          refreshDeploymentContext($scope.deploymentContext, $scope.application, deploymentTopologyServices, deploymentTopologyProcessor, tasksProcessor, menu).then(function() {
            //finally, go to the next invalid step
            goToNextInvalidStep();
          });
        }

        $scope.onEnvironmentChange = function() {
          // update the global environment
          appEnvironments.select($scope.deploymentContext.selectedEnvironment.id, refreshDeploymentSetupStatus);
        };

        refreshDeploymentSetupStatus(); // immediately refresh setup status and go to the next invalid tab

        $scope.showTodoList = function() {
          return $scope.validTopologyDTOLoaded && !$scope.validTopologyDTO.valid && $scope.isManager;
        };

        $scope.showWarningList = function() {
          return ($scope.validTopologyDTOLoaded && angular.isObject($scope.validTopologyDTO.warningList) && Object.keys($scope.validTopologyDTO.warningList).length > 0) ||
            ($scope.deploymentContext && angular.isObject($scope.deploymentContext.deploymentTopologyDTO) && Object.keys($scope.deploymentContext.deploymentTopologyDTO.validation) && $scope.deploymentContext.deploymentTopologyDTO.validation.warningList && Object.keys($scope.deploymentContext.deploymentTopologyDTO.validation.warningList).length > 0);
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
          $scope.stopEvent();
          // no registration for this environement -> register if not undeployed!
          // if status the application is not undeployed we should register for events.
          if (newValue !== undeployedValue) {
            $scope.setTopologyIdFromEnvironment($scope.deploymentContext.selectedEnvironment);
            checkTopology();
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

        $scope.goToTopologyValidation = function(){
          $state.go('applications.detail.topology.validation');
        };

      }
    ]); //controller
}); //Define
