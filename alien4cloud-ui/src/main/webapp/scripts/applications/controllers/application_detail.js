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
  require('scripts/applications/services/application_environment_builder');

  require('scripts/applications/controllers/application_info');
  require('scripts/applications/controllers/application_topology');
  require('scripts/applications/controllers/application_topology_editor');
  require('scripts/applications/controllers/application_deployment');
  require('scripts/applications/controllers/application_environments');
  require('scripts/applications/controllers/application_versions');
  require('scripts/applications/controllers/application_users');
  require('scripts/applications/controllers/runtime');

  require('scripts/layout/resource_layout');

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
      appEnvironments: ['application', 'appEnvironmentsBuilder', '$stateParams',
        function(application, appEnvironmentsBuilder, $stateParams) {
          return appEnvironmentsBuilder(application.data, $stateParams.openOnEnvironment);
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
      ],
    },
    templateUrl: 'views/applications/vertical_menu_layout.html',
    controller: 'ApplicationCtrl',
    params: {
      // optional id of the environment to automatically select when triggering this state
      openOnEnvironment: null
    }
  });

  // definition of the parent controller and scope for application management.
  modules.get('a4c-applications').controller('ApplicationCtrl',
    ['$controller','$rootScope', '$scope', 'menu', 'authService', 'resourceLayoutService', 'application', 'appEnvironments', 'environmentEventServicesFactory', 'topologyServices', 'applicationServices', 'applicationEventServicesFactory', 'topologyJsonProcessor', 'toscaService', '$translate', 'toaster',
    function($controller, $rootScope, $scope, menu, authService, resourceLayoutService, applicationResult, appEnvironments,
      environmentEventServicesFactory, topologyServices, applicationServices, applicationEventServicesFactory, topologyJsonProcessor, toscaService, $translate, toaster) {

      var application = applicationResult.data;
      if (!_.has(application.userRoles, authService.currentStatus.data.username)) {
        application.userRoles = application.userRoles || {};
        application.userRoles[authService.currentStatus.data.username] = [];
      }
      $scope.application = application;

      $controller('ResourceLayoutCtrl', {$scope: $scope, menu: menu, resourceLayoutService: resourceLayoutService, resource: application});

      /**
      The application controller manages two UI Roles that are injected based on selected environment and runtime status
       - APPLICATION_DEPLOYER if the user has deployment role on the selected environment
      */

      // Application rights
      var isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
      // Application environment rights. Manager has right anyway, for other users we check all environments (see below)
      var runtimeMenuItem = _.find(menu, {'id': 'am.applications.detail.runtime'});
      var isDeployer = isManager;

      /** RUNTIME UPDATES MANAGEMENT */
      function updateRuntimeDisabled() {
        // get newest environments status, disable the menu if none of the deployEnvironments is deployed
        var disabled = _.undefined(_.find(appEnvironments.deployEnvironments, function(env){
          return env.status !== 'UNDEPLOYED' && env.status !== 'UNKNOWN';
        }));

        runtimeMenuItem.disabled = disabled;
      }

      function onDeployerChanged() {
        var deployerRoleIndex = _.indexOf(application.userRoles[authService.currentStatus.data.username], 'APPLICATION_DEPLOYER');
        if(isDeployer) {
          if(deployerRoleIndex === -1) {
            application.userRoles[authService.currentStatus.data.username].push('APPLICATION_DEPLOYER');
          }
        } else {
          if(deployerRoleIndex !== -1) {
            application.userRoles[authService.currentStatus.data.username].splice(deployerRoleIndex, 1);
          }
        }

        updateRuntimeDisabled();

        $scope.updateMenu();
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

      var deploymentStatusCallback = function(environment, event) {
        environment.status = event.deploymentStatus;
        displayDeploymentStatusToaster(environment);
        updateRuntimeDisabled();
        // update the current scope and it's child scopes.
        $scope.$digest();
      };

      /** ENVIRONMENTS MANAGEMENT */
      //TODO move all of these into appEnvironmentsBuilder
      // registrations for environment events
      appEnvironments.eventRegistrations = [];

      function registerEnvironment(environment) {
        var registration = environmentEventServicesFactory(application.id, environment, deploymentStatusCallback);
        appEnvironments.eventRegistrations.push(registration);
        var isEnvDeployer = authService.hasResourceRole(environment, 'DEPLOYMENT_MANAGER');
        if ((isManager || isEnvDeployer) && _.indexOf(appEnvironments.deployEnvironments, environment) < 0) {
          appEnvironments.deployEnvironments.push(environment);
        }
        isDeployer = isDeployer || isEnvDeployer;
      }

      appEnvironments.removeEnvironment = function(environmentId) {
        var envIndex = _.findIndex(appEnvironments.environments, 'id', environmentId);
        if (envIndex !== -1) { // remove the environment
          appEnvironments.eventRegistrations[envIndex].close(); // close registration
          appEnvironments.environments.splice(envIndex, 1);
          appEnvironments.eventRegistrations.splice(envIndex, 1);
        }
        // eventually remove the environment from deployable environments
        envIndex = _.findIndex(appEnvironments.deployEnvironments, 'id', environmentId);
        if (envIndex !== -1) {
          appEnvironments.deployEnvironments.splice(envIndex, 1);
        }
        //eventually if it was the seleted one, then select another one
        if(appEnvironments.selected.id === environmentId){
          appEnvironments.selected = null;
          if(_.isNotEmpty(appEnvironments.environments)){
            appEnvironments.select(appEnvironments.environments[0].id);
          }
        }
        // user may not be deployer anymore
        var isDeployer = isManager;
        var i=0;
        while(!isDeployer && i < appEnvironments.environments.length) {
          isDeployer = authService.hasResourceRole(appEnvironments.environments[i], 'DEPLOYMENT_MANAGER');
          i++;
        }
        onDeployerChanged();
      };

      appEnvironments.addEnvironment = function(environment) {
        appEnvironments.environments.push(environment);
        registerEnvironment(environment);
        onDeployerChanged();
      };

      // for every environement register for deployment status update for enrichment.
      for (var i = 0; i < appEnvironments.environments.length; i++) {
        var environment = appEnvironments.environments[i];
        registerEnvironment(environment);
      }
      onDeployerChanged();
      // get environments
      $scope.envs = appEnvironments.environments;

      // Stop listening if deployment active exists
      $scope.$on('$destroy', function() {
        for (var i = 0; i < appEnvironments.eventRegistrations.length; i++) {
          appEnvironments.eventRegistrations[i].close();
        }
      });

      $scope.isNotEmpty = _.isNotEmpty;

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
        $scope.inputArtifacts = result.data.topology.inputArtifacts;

        $scope.inputsSize = 0;
        $scope.inputArtifactsSize = 0;

        if (angular.isDefined(result.data.topology.inputs)) {
          $scope.inputsSize = Object.keys(result.data.topology.inputs).length;
        }
        if (angular.isDefined(result.data.topology.inputArtifacts)) {
          $scope.inputArtifactsSize = Object.keys(result.data.topology.inputArtifacts).length;
        }
      };

      // fetch the topology to display intput properties and matching data
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

      $scope.processDeploymentTopologyInformation = function processDeploymentTopologyInformation() {
        return applicationServices.getRuntimeTopology.get({
          applicationId: application.id,
          applicationEnvironmentId: appEnvironments.selected.id
        }, function(result) {
          topologyJsonProcessor.process(result.data);
          $scope.outputProperties = result.data.topology.outputProperties;
          $scope.outputCapabilityProperties = result.data.topology.outputCapabilityProperties;
          $scope.outputAttributes = result.data.topology.outputAttributes;
          $scope.nodeTemplates = result.data.topology.nodeTemplates;
          $scope.outputNodes = [];
          $scope.outputPropertiesSize = 0;
          $scope.outputAttributesSize = 0;
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
        }, function() {
          delete $scope.outputProperties;
          delete $scope.outputCapabilityProperties;
          delete $scope.outputAttributes;
          delete $scope.nodeTemplates;
          delete $scope.outputNodes;
          $scope.outputPropertiesSize = 0;
          $scope.outputAttributesSize = 0;
        });
      };

      // get a topologyId
      $scope.setTopologyIdFromEnvironment = function(environment) {
        $scope.topologyId = environment.applicationId + ':' + environment.currentVersionName;
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
