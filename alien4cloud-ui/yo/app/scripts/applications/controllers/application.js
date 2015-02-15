/* global UTILS */
'use strict';

angular.module('alienUiApp').controller('ApplicationCtrl', ['$rootScope', '$scope', 'alienAuthService', 'application', '$state', 'applicationEnvironmentServices', 'appEnvironments', 'environmentEventServicesFactory', 'topologyServices', 'applicationServices', 'applicationEventServicesFactory',
  function($rootScope, $scope, alienAuthService, applicationResult, $state, applicationEnvironmentServices, appEnvironments,
    environmentEventServicesFactory, topologyServices, applicationServices, applicationEventServicesFactory) {
    var application = applicationResult.data;
    $scope.application = application;

    // Application rights
    var isManager = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    var isDevops = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
    var isUser = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_USER');
    // Application environment rights. Manager has right anyway, for other users we check all environments (see below)
    var isDeployer = isManager;

    var runtimeMenuItem = {
      id: 'am.applications.detail.runtime',
      state: 'applications.detail.runtime',
      key: 'NAVAPPLICATIONS.MENU_RUNTIME',
      icon: 'fa fa-cogs',
      show: false
    };

    function updateRuntimeDisabled() {
      // get newest environments statuses
      var disabled = true;
      for (var i = 0; i < appEnvironments.environments.length && disabled; i++) {
        if (!(appEnvironments.environments[i].status === 'UNDEPLOYED' || appEnvironments.environments[i].status === 'UNKNOWN')) {
          disabled = false;
        }
      }
      runtimeMenuItem.show = (isManager || isDeployer);
      runtimeMenuItem.disabled = disabled;
    }

    var callback = function(environment, event) {
      environment.status = event.deploymentStatus;
      updateRuntimeDisabled();
      // update the current scope and it's child scopes.
      $scope.$digest();
    };

    function registerEnvironment(environment) {
      var registration = environmentEventServicesFactory(application.id, environment, callback);
      appEnvironments.eventRegistrations.push(registration);
      var isEnvDeployer = alienAuthService.hasResourceRole(environment, 'DEPLOYMENT_MANAGER');
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

    // for every environement register for deployment status update for enrichment.
    for (var i = 0; i < appEnvironments.environments.length; i++) {
      var environment = appEnvironments.environments[i];
      var registration = environmentEventServicesFactory(application.id, environment, callback);
      appEnvironments.eventRegistrations.push(registration);
      var isEnvDeployer = alienAuthService.hasResourceRole(environment, 'DEPLOYMENT_MANAGER');
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

    $scope.isMapEmpty = UTILS.isMapNotNullOrEmpty;

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

    // fetch the topology to display intput/output properties and matching data
    $scope.processTopologyInformations = function processTopologyInformations(topologyId) {

      return topologyServices.dao.get({
        topologyId: topologyId
      }, function(result) {
        $scope.topologyDTO = result.data;
        // initialize compute and network icons from the actual tosca types (to match topology representation).
        if (UTILS.isDefinedAndNotNull($scope.topologyDTO.nodeTypes['tosca.nodes.Compute']) &&
          UTILS.isArrayDefinedAndNotEmpty($scope.topologyDTO.nodeTypes['tosca.nodes.Compute'].tags)) {
          $scope.computeImage = UTILS.getIcon($scope.topologyDTO.nodeTypes['tosca.nodes.Compute'].tags);
        }
        if (UTILS.isDefinedAndNotNull($scope.topologyDTO.nodeTypes['tosca.nodes.Network']) &&
          UTILS.isArrayDefinedAndNotEmpty($scope.topologyDTO.nodeTypes['tosca.nodes.Network'].tags)) {
          $scope.networkImage = UTILS.getIcon($scope.topologyDTO.nodeTypes['tosca.nodes.Network'].tags);
        }
        if (UTILS.isDefinedAndNotNull($scope.topologyDTO.nodeTypes['tosca.nodes.BlockStorage']) &&
          UTILS.isArrayDefinedAndNotEmpty($scope.topologyDTO.nodeTypes['tosca.nodes.BlockStorage'].tags)) {
          $scope.storageImage = UTILS.getIcon($scope.topologyDTO.nodeTypes['tosca.nodes.BlockStorage'].tags);
        }
        // process topology data
        $scope.inputProperties = result.data.topology.inputProperties;
        $scope.outputProperties = result.data.topology.outputProperties;
        $scope.outputAttributes = result.data.topology.outputAttributes;
        $scope.inputArtifacts = result.data.topology.inputArtifacts;
        $scope.nodeTemplates = $scope.topologyDTO.topology.nodeTemplates;
        $scope.outputNodes = [];
        $scope.inputPropertiesSize = 0;
        $scope.outputPropertiesSize = 0;
        $scope.outputAttributesSize = 0;
        $scope.inputArtifactsSize = 0;

        if (angular.isDefined(result.data.topology.inputProperties)) {
          $scope.inputPropertiesSize = Object.keys(result.data.topology.inputProperties).length;
        }
        if (angular.isDefined($scope.outputProperties)) {
          $scope.outputNodes = Object.keys($scope.outputProperties);
          $scope.outputPropertiesSize = Object.keys($scope.outputProperties).length;
          refreshOutputProperties();
        }
        if (angular.isDefined($scope.outputAttributes)) {
          $scope.outputNodes = UTILS.arrayUnique(UTILS.concat($scope.outputNodes, Object.keys($scope.outputAttributes)));
          $scope.outputAttributesSize = Object.keys($scope.outputAttributes).length;
        }
        if (angular.isDefined(result.data.topology.inputArtifacts)) {
          $scope.inputArtifactsSize = Object.keys(result.data.topology.inputArtifacts).length;
        }

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
          if (UTILS.isDefinedAndNotNull($scope.topologyId)) {
            successTopologyIdCallBack();
          }
        }
      });
    };

    $scope.stopEvent = function stopEvent() {
      $scope.outputAttributesValue = {};
      $scope.outputPropertiesValue = {};
      $scope.outputNodes = [];
      $scope.inputPropertiesSize = 0;
      $scope.outputPropertiesSize = 0;
      $scope.outputAttributesSize = 0;
      $scope.inputArtifactsSize = 0;
      if ($scope.applicationEventServices !== null) {
        $scope.applicationEventServices.stop();
        $scope.applicationEventServices = null;
      }
    };

    $scope.applicationEventServices = null;
    $scope.outputAttributesValue = {};
    $scope.outputPropertiesValue = {};
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
      if (UTILS.isUndefinedOrNull($scope[type])) {
        return false;
      }
      if (!$scope[type].hasOwnProperty(nodeId)) {
        return false;
      }
      return $scope[type][nodeId].indexOf(propertyName) >= 0;
    };

    // link output properties based on values that exists in the topology's node templates.
    function refreshOutputProperties() {
      for (var nodeId in $scope.outputProperties) {
        if ($scope.outputProperties.hasOwnProperty(nodeId)) {
          $scope.outputPropertiesValue[nodeId] = {};
          for (var i = 0; i < $scope.outputProperties[nodeId].length; i++) {
            var outputPropertyName = $scope.outputProperties[nodeId][i];
            $scope.outputPropertiesValue[nodeId][outputPropertyName] = $scope.nodeTemplates[nodeId].properties[outputPropertyName];
          }
        }
      }
    }

    var onInstanceStateChange = function(type, event) {
      if (UTILS.isUndefinedOrNull(event.instanceState)) {
        // Delete event
        if (UTILS.isDefinedAndNotNull($scope.outputAttributesValue[event.nodeTemplateId])) {
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
            if (UTILS.isUndefinedOrNull($scope.outputAttributesValue[event.nodeTemplateId])) {
              $scope.outputAttributesValue[event.nodeTemplateId] = {};
            }
            if (UTILS.isUndefinedOrNull($scope.outputAttributesValue[event.nodeTemplateId][event.instanceId])) {
              $scope.outputAttributesValue[event.nodeTemplateId][event.instanceId] = {};
            }
            // fill up OUTPUT ATTRIBUTES
            $scope.outputAttributesValue[event.nodeTemplateId][event.instanceId][attribute] = allAttributes[attribute];
          }
        }
      }
      $scope.$apply();
    };

    var doSubscribe = function doSubscribe(appRuntimeInformation, stateId)  {
      $scope.applicationEventServices.subscribeToInstanceStateChange(stateId, onInstanceStateChange);
      if (UTILS.isDefinedAndNotNull(appRuntimeInformation)) {
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

    $scope.menu = [{
      id: 'am.applications.info',
      state: 'applications.detail.info',
      key: 'NAVAPPLICATIONS.MENU_INFO',
      icon: 'fa fa-info',
      show: (isManager || isDeployer || isDevops || isUser)
    }, {
      id: 'am.applications.detail.topology',
      state: 'applications.detail.topology',
      key: 'NAVAPPLICATIONS.MENU_TOPOLOGY',
      icon: 'fa fa-sitemap',
      show: (isManager || isDevops)
    }, {
      id: 'am.applications.detail.deployment',
      state: 'applications.detail.deployment',
      key: 'NAVAPPLICATIONS.MENU_DEPLOYMENT',
      icon: 'fa fa-cloud-upload',
      show: (isManager || isDeployer)
    }, runtimeMenuItem, {
      id: 'am.applications.detail.versions',
      state: 'applications.detail.versions',
      key: 'NAVAPPLICATIONS.MENU_VERSIONS',
      icon: 'fa fa-tasks',
      show: isManager
    }, {
      id: 'am.applications.detail.environments',
      state: 'applications.detail.environments',
      key: 'NAVAPPLICATIONS.MENU_ENVIRONMENT',
      icon: 'fa fa-share-alt',
      show: isManager
    }, {
      id: 'am.applications.detail.users',
      state: 'applications.detail.users',
      key: 'NAVAPPLICATIONS.MENU_USERS',
      icon: 'fa fa-users',
      show: isManager
    }];
  }
]);
