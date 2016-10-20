define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/applications/services/application_services');
  require('scripts/applications/services/application_environment_services');
  require('scripts/applications/services/application_version_services');
  require('scripts/applications/services/environment_event_services');
  require('scripts/applications/services/application_event_services');

  require('scripts/applications/controllers/application_info');
  require('scripts/applications/controllers/application_topology');
  require('scripts/applications/controllers/application_topology_editor');
  require('scripts/applications/controllers/application_deployment');
  require('scripts/applications/controllers/application_environments');
  require('scripts/applications/controllers/application_versions');
  require('scripts/applications/controllers/application_users');
  require('scripts/applications/controllers/topology_runtime');
  require('scripts/applications/controllers/application_runtime');

  states.state('applications.detail', {
    url: '/details/:id',
    resolve: {
      application: ['applicationServices', '$stateParams',
        function(applicationServices, $stateParams) {
          return applicationServices.get({
            applicationId: $stateParams.id
          }).$promise;
        }
      ],
      appEnvironments: ['application', 'applicationEnvironmentServices',
        function(application, applicationEnvironmentServices) {
          return applicationEnvironmentServices.getAllEnvironments(application.data.id).then(function(result) {
            var environments = result.data.data;
            var selected;
            if(environments) {
              if(environments.length > 0) {
                selected = environments[0];
                selected.active = true;
              }
            } else {
              environments = [];
            }
            return {
              environments: environments,
              selected: selected
            };
          });
        }
      ],
      archiveVersions: ['$http', 'application', 'applicationVersionServices',
        function($http, application, applicationVersionServices) {
          var searchAppVersionRequestObject = {
            'from': 0,
            'size': 400
          };
          return applicationVersionServices.searchVersion({
            delegateId: application.data.id
          }, angular.toJson(searchAppVersionRequestObject)).$promise.then(function(result) {
            return result.data;
          });
        }
      ]
    },
    templateUrl: 'views/applications/vertical_menu_layout.html',
    controller: 'ApplicationCtrl',
    params: {
      // optional id of the environment to automatically select when triggering this state
      openOnEnvironment:null
    }
  });

  // definition of the parent controller and scope for application management.
  modules.get('a4c-applications').controller('ApplicationCtrl',
    ['$rootScope', '$scope', 'menu', 'authService', 'application', '$state', 'applicationEnvironmentServices', 'appEnvironments', 'environmentEventServicesFactory', 'topologyServices', 'applicationServices', 'applicationEventServicesFactory', 'topologyJsonProcessor', 'toscaService', '$translate', 'toaster',
    function($rootScope, $scope, menu, authService, applicationResult, $state, applicationEnvironmentServices, appEnvironments,
      environmentEventServicesFactory, topologyServices, applicationServices, applicationEventServicesFactory, topologyJsonProcessor, toscaService, $translate, toaster) {

      var application = applicationResult.data;
      $scope.application = application;

      /** ROLES MANAGEMENT */
      var runtimeMenuItem;
      var deploymentMenuItem;
      _.each(menu, function(menuItem) {
        if(menuItem.id === 'am.applications.detail.runtime') {
          runtimeMenuItem = menuItem;
        }
        if(menuItem.id === 'am.applications.detail.deployment') {
          deploymentMenuItem = menuItem;
        }
        if(_.has(menuItem, 'roles')) {
          menuItem.show = authService.hasResourceRoleIn(application, menuItem.roles);
        } else { // if there is no roles requirement then the menu is visible
          menuItem.show = true;
        }
      });
      $scope.menu = menu;

      // Application rights
      var isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
      // Application environment rights. Manager has right anyway, for other users we check all environments (see below)
      var isDeployer = isManager;

      /** RUNTIME UPDATES MANAGEMENT */
      function updateRuntimeDisabled() {
        // get newest environments statuses
        var disabled = true;
        for (var i = 0; i < appEnvironments.environments.length && disabled; i++) {
          if (!(appEnvironments.environments[i].status === 'UNDEPLOYED' || appEnvironments.environments[i].status === 'UNKNOWN')) {
            disabled = false;
          }
        }
        deploymentMenuItem.show = isDeployer;
        runtimeMenuItem.show = isDeployer;
        runtimeMenuItem.disabled = disabled;
      }

      function displayDeploymentStatusToaster(environment) {
        if (environment.status === 'FAILURE') {
          toaster.pop(
            'error',
            $translate.instant('DEPLOYMENT.STATUS.FAILURE'),
            $translate.instant('DEPLOYMENT.TOASTER_STATUS.FAILURE', {
              envName : environment.name,
              appName : $scope.application.name
            }),
            6000, 'trustedHtml', null
          );
        } else if (environment.status === 'DEPLOYED') {
          toaster.pop(
            'success',
            $translate.instant('DEPLOYMENT.STATUS.DEPLOYED'),
            $translate.instant('DEPLOYMENT.TOASTER_STATUS.DEPLOYED', {
              envName : environment.name,
              appName : $scope.application.name
            }),
            4000, 'trustedHtml', null
          );
        }
      }

      var callback = function(environment, event) {
        environment.status = event.deploymentStatus;
        displayDeploymentStatusToaster(environment);
        updateRuntimeDisabled();
        // update the current scope and it's child scopes.
        $scope.$digest();
      };

      /** ENVIRONMENTS MANAGEMENT */
      function registerEnvironment(environment) {
        var registration = environmentEventServicesFactory(application.id, environment, callback);
        appEnvironments.eventRegistrations.push(registration);
        var isEnvDeployer = authService.hasResourceRole(environment, 'DEPLOYMENT_MANAGER');
        if (isManager || isEnvDeployer) {
          appEnvironments.deployEnvironments.push(environment);
        }
        isDeployer = isDeployer || isEnvDeployer;
      }

      appEnvironments.deployEnvironments = [];
      appEnvironments.eventRegistrations = [];
      appEnvironments.removeEnvironment = function(environmentId) {
        var envIndex = null,
          i;
        for (i = 0; i < appEnvironments.environments.length && envIndex === null; i++) {
          if (appEnvironments.environments[i].id === environmentId) {
            envIndex = i;
          }
        }
        if (envIndex !== null) {
          appEnvironments.environments.splice(envIndex, 1);
          appEnvironments.eventRegistrations.splice(envIndex, 1);
        }
        // eventually remove the environment from deployable environments
        envIndex = null;
        for (i = 0; i < appEnvironments.deployEnvironments.length && envIndex === null; i++) {
          if (appEnvironments.deployEnvironments[i].id === environmentId) {
            envIndex = i;
          }
        }
        if (envIndex !== null) {
          appEnvironments.deployEnvironments.splice(envIndex, 1);
        }

        //eventually if it was the seleted one, then select another one
        if(appEnvironments.selected.id === environmentId){
          appEnvironments.selected = null;
          if(_.isNotEmpty(appEnvironments.environments)){
            appEnvironments.select(appEnvironments.environments[0].id);
          }
        }
      };

      appEnvironments.addEnvironment = function(environment) {
        appEnvironments.environments.push(environment);
        registerEnvironment(environment);
        updateRuntimeDisabled();
      };

      appEnvironments.updateEnvironment = function(environment) {
        // replace the environment with the one given as a parameter.
        var envIndex = null;
        for (i = 0; i < appEnvironments.environments.length && envIndex === null; i++) {
          if (appEnvironments.environments[i].id === environment.id) {
            envIndex = i;
          }
        }
        if (envIndex !== null) {
          appEnvironments.environments.splice(envIndex, 1, environment);
        }
        envIndex = null;
        for (i = 0; i < appEnvironments.deployEnvironments.length && envIndex === null; i++) {
          if (appEnvironments.deployEnvironments[i].id === environment.id) {
            envIndex = i;
          }
        }
        if (envIndex !== null) {
          appEnvironments.deployEnvironments.splice(envIndex, 1, environment);
        }
      };
      //
      appEnvironments.select = function(environmentId, envChangedCallback, force) {
        if(_.defined(appEnvironments.selected)){
          if(appEnvironments.selected.id === environmentId && !force) {
            return; // the environement is already selected.
          }
          appEnvironments.selected.active = false;
        }
        for (i = 0; i < appEnvironments.environments.length; i++) {
          if (appEnvironments.environments[i].id === environmentId) {
            appEnvironments.selected = appEnvironments.environments[i];
            appEnvironments.selected.active = true;
            if(_.defined(envChangedCallback)) {
              envChangedCallback();
            }
          }
        }
      };

      // for every environement register for deployment status update for enrichment.
      for (var i = 0; i < appEnvironments.environments.length; i++) {
        var environment = appEnvironments.environments[i];
        var registration = environmentEventServicesFactory(application.id, environment, callback);
        appEnvironments.eventRegistrations.push(registration);
        var isEnvDeployer = authService.hasResourceRole(environment, 'DEPLOYMENT_MANAGER');
        if (isManager || isEnvDeployer) {
          appEnvironments.deployEnvironments.push(environment);
        }
        isDeployer = isDeployer || isEnvDeployer;
      }
      updateRuntimeDisabled();
      // get environments
      $scope.envs = appEnvironments.environments;

      // Stop listening if deployment active exists
      $scope.$on('$destroy', function() {
        for (var i = 0; i < appEnvironments.eventRegistrations.length; i++) {
          appEnvironments.eventRegistrations[i].close();
        }
      });

      $scope.onItemClick = function($event, menuItem) {
        if (menuItem.disabled) {
          $event.preventDefault();
          $event.stopPropagation();
        }
      };

      $scope.isMapEmpty = _.isNotEmpty;

      // TOPOLOGY INFO CONCERNS
      // verify the topology validity
      $scope.isTopologyValid = function isTopologyValid(topologyId) {
        // validate the topology
        return topologyServices.isValid({
          topologyId: topologyId
        }, function(result) {
          return result.data;
        });
      };

      var doProcessTopologyInfos = function(result){
        $scope.topologyDTO = result.data;
        topologyJsonProcessor.process($scope.topologyDTO);
        // initialize compute and network icons from the actual tosca types (to match topology representation).
        if (_.defined($scope.topologyDTO.nodeTypes['tosca.nodes.Compute']) &&
          _.isNotEmpty($scope.topologyDTO.nodeTypes['tosca.nodes.Compute'].tags)) {
          $scope.computeImage = toscaService.getIcon($scope.topologyDTO.nodeTypes['tosca.nodes.Compute'].tags);
        }
        if (_.defined($scope.topologyDTO.nodeTypes['tosca.nodes.Network']) &&
          _.isNotEmpty($scope.topologyDTO.nodeTypes['tosca.nodes.Network'].tags)) {
          $scope.networkImage = toscaService.getIcon($scope.topologyDTO.nodeTypes['tosca.nodes.Network'].tags);
        }
        if (_.defined($scope.topologyDTO.nodeTypes['tosca.nodes.BlockStorage']) &&
          _.isNotEmpty($scope.topologyDTO.nodeTypes['tosca.nodes.BlockStorage'].tags)) {
          $scope.storageImage = toscaService.getIcon($scope.topologyDTO.nodeTypes['tosca.nodes.BlockStorage'].tags);
        }
        // process topology data
        $scope.inputs = result.data.topology.inputs;
        $scope.outputProperties = result.data.topology.outputProperties;
        $scope.outputCapabilityProperties = result.data.topology.outputCapabilityProperties;
        $scope.outputAttributes = result.data.topology.outputAttributes;
        $scope.inputArtifacts = result.data.topology.inputArtifacts;
        $scope.nodeTemplates = $scope.topologyDTO.topology.nodeTemplates;
        $scope.nodeTypes = $scope.topologyDTO.nodeTypes;
        $scope.outputNodes = [];
        $scope.inputsSize = 0;
        $scope.outputPropertiesSize = 0;
        $scope.outputAttributesSize = 0;
        $scope.inputArtifactsSize = 0;

        if (angular.isDefined(result.data.topology.inputs)) {
          $scope.inputsSize = Object.keys(result.data.topology.inputs).length;
        }
        if (angular.isDefined($scope.outputProperties)) {
          $scope.outputNodes = Object.keys($scope.outputProperties);
          $scope.outputPropertiesSize = Object.keys($scope.outputProperties).length;
          refreshOutputProperties();
        }
        if (angular.isDefined($scope.outputCapabilityProperties)) {
          $scope.outputNodes = _.union($scope.outputNodes, Object.keys($scope.outputCapabilityProperties));
          refreshOutputProperties();
        }
        if (angular.isDefined($scope.outputAttributes)) {
          $scope.outputNodes = _.union($scope.outputNodes, Object.keys($scope.outputAttributes));
          $scope.outputAttributesSize = Object.keys($scope.outputAttributes).length;
        }
        if (angular.isDefined(result.data.topology.inputArtifacts)) {
          $scope.inputArtifactsSize = Object.keys(result.data.topology.inputArtifacts).length;
        }
      };

      // fetch the topology to display intput/output properties and matching data
      $scope.processTopologyInformations = function processTopologyInformations(topologyId) {

        return topologyServices.dao.get({
          topologyId: topologyId
        }, function(result) {
          if(_.undefined(result.error)){
            doProcessTopologyInfos(result);
            return;
          }
          //case there actually is an error
          //TODO what should we do here? redirect to the editor view ? in that case make sure the user has the correct rights

        });

      };

      // get a topologyId
      $scope.setTopologyId = function setTopologyId(applicationId, environmentId, successTopologyIdCallBack) {
        return applicationEnvironmentServices.getTopologyId({
          applicationId: applicationId,
          applicationEnvironmentId: environmentId
        }, undefined, function(response) {
          $scope.topologyId = response.data;
          if (successTopologyIdCallBack) {
            if (_.defined($scope.topologyId)) {
              successTopologyIdCallBack();
            }
          }
        });
      };

      $scope.stopEvent = function stopEvent() {
        $scope.outputAttributesValue = {};
        $scope.outputPropertiesValue = {};
        $scope.outputCapabilityPropertiesValue = {};
        $scope.outputNodes = [];
        if ($scope.applicationEventServices !== null) {
          $scope.applicationEventServices.stop();
          $scope.applicationEventServices = null;
        }
      };

      $scope.applicationEventServices = null;
      $scope.outputAttributesValue = {};
      $scope.outputPropertiesValue = {};
      $scope.outputCapabilityPropertiesValue = {};
      $scope.outputNodes = [];

      $scope.refreshInstancesStatuses = function refreshInstancesStatuses(applicationId, environmentId, pageStateId) {
        if ($scope.outputAttributesSize > 0) {
          applicationServices.runtime.get({
            applicationId: applicationId,
            applicationEnvironmentId: environmentId
          }, function(successResult) {
            // start event listener on this new <app,env>
            $scope.applicationEventServices = applicationEventServicesFactory(applicationId, environmentId);
            $scope.applicationEventServices.start();
            doSubscribe(successResult.data, pageStateId);
          });
        }
      };

      var isOutput = function(nodeId, propertyName, type) {
        if (_.undefined($scope[type])) {
          return false;
        }
        if (!$scope[type].hasOwnProperty(nodeId)) {
          return false;
        }
        return $scope[type][nodeId].indexOf(propertyName) >= 0;
      };

      // link output properties based on values that exists in the topology's node templates.
      function refreshOutputProperties() {
        var i;
        for (var nodeId in $scope.outputProperties) {
          if ($scope.outputProperties.hasOwnProperty(nodeId)) {
            $scope.outputPropertiesValue[nodeId] = {};
            for (i = 0; i < $scope.outputProperties[nodeId].length; i++) {
              var outputPropertyName = $scope.outputProperties[nodeId][i];
              $scope.outputPropertiesValue[nodeId][outputPropertyName] = $scope.nodeTemplates[nodeId].propertiesMap[outputPropertyName].value;
            }
          }
        }

        if (!_.undefined($scope.outputCapabilityProperties)) {
          for (nodeId in $scope.outputCapabilityProperties) {
            if ($scope.outputCapabilityProperties.hasOwnProperty(nodeId)) {
              $scope.outputCapabilityPropertiesValue[nodeId] = {};
              for (var capabilityId in $scope.outputCapabilityProperties[nodeId]) {
                if ($scope.outputCapabilityProperties[nodeId].hasOwnProperty(capabilityId)) {
                  $scope.outputCapabilityPropertiesValue[nodeId][capabilityId] = {};
                  for (i = 0; i < $scope.outputCapabilityProperties[nodeId][capabilityId].length; i++) {
                    var outputCapabilityPropertyName = $scope.outputCapabilityProperties[nodeId][capabilityId][i];
                    $scope.outputCapabilityPropertiesValue[nodeId][capabilityId][outputCapabilityPropertyName] = $scope.nodeTemplates[nodeId].capabilitiesMap[capabilityId].value.propertiesMap[outputCapabilityPropertyName].value;
                  }
                }
              }
            }
          }
        }
      }

      var onInstanceStateChange = function(type, event) {
        if (_.undefined(event.instanceState)) {
          // Delete event
          if (_.defined($scope.outputAttributesValue[event.nodeTemplateId])) {
            delete $scope.outputAttributesValue[event.nodeTemplateId][event.instanceId];
            if (Object.keys($scope.outputAttributesValue[event.nodeTemplateId]).length === 0) {
              delete $scope.outputAttributesValue[event.nodeTemplateId];
            }
          }
        } else {
          // Add modify event
          var allAttributes = event.attributes;
          for (var attribute in allAttributes) {
            if (allAttributes.hasOwnProperty(attribute) && isOutput(event.nodeTemplateId, attribute, 'outputAttributes')) {
              if (_.undefined($scope.outputAttributesValue[event.nodeTemplateId])) {
                $scope.outputAttributesValue[event.nodeTemplateId] = {};
              }
              if (_.undefined($scope.outputAttributesValue[event.nodeTemplateId][event.instanceId])) {
                $scope.outputAttributesValue[event.nodeTemplateId][event.instanceId] = {};
              }
              // fill up OUTPUT ATTRIBUTES
              $scope.outputAttributesValue[event.nodeTemplateId][event.instanceId][attribute] = allAttributes[attribute];
            }
          }
        }
        $scope.$digest();
      };

      var doSubscribe = function doSubscribe(appRuntimeInformation, stateId) {
        $scope.applicationEventServices.subscribeToInstanceStateChange(stateId, onInstanceStateChange);
        if (_.defined(appRuntimeInformation)) {
          for (var nodeId in appRuntimeInformation) {
            if (appRuntimeInformation.hasOwnProperty(nodeId)) {
              $scope.outputAttributesValue[nodeId] = {};
              var nodeInformation = appRuntimeInformation[nodeId];
              for (var instanceId in nodeInformation) {
                if (nodeInformation.hasOwnProperty(instanceId)) {
                  $scope.outputAttributesValue[nodeId][instanceId] = {};
                  var allAttributes = nodeInformation[instanceId].attributes;
                  for (var attribute in allAttributes) {
                    if (allAttributes.hasOwnProperty(attribute) && isOutput(nodeId, attribute, 'outputAttributes')) {
                      $scope.outputAttributesValue[nodeId][instanceId][attribute] = allAttributes[attribute];
                    }
                  }
                  if (Object.keys($scope.outputAttributesValue[nodeId][instanceId]).length === 0) {
                    delete $scope.outputAttributesValue[nodeId][instanceId];
                  }
                }
              }
              var nbOfInstances = Object.keys($scope.outputAttributesValue[nodeId]).length;
              if (nbOfInstances === 0) {
                delete $scope.outputAttributesValue[nodeId];
              }
            }
          }
        }
      };
      $scope.doSubscribe = doSubscribe;
    }
  ]);
});
