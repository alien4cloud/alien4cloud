define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');
  require('d3');

  require('scripts/common/services/resize_services');

  require('scripts/topology/controllers/topology_editor_display');
  require('scripts/topology/controllers/topology_editor_workflows');
  require('scripts/topology/directives/workflow_rendering');

  states.state('applications.detail.runtime.topology', {
    url: '/runtime/topology',
    templateUrl: 'views/topology/topology_runtime.html',
    controller: 'TopologyRuntimeCtrl',
    menu: {
      id: 'am.applications.detail.runtime.topology',
      state: 'applications.detail.runtime.topology',
      key: 'NAVAPPLICATIONS.MENU_RUNTIME',
      icon: 'fa fa-cogs',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer
      priority: 400
    }
  });
  states.forward('applications.detail.runtime', 'applications.detail.runtime.topology');

  modules.get('a4c-applications').controller('TopologyRuntimeCtrl',
    ['$scope',
    'applicationServices',
    '$translate',
    'deploymentServices',
    'applicationEventServicesFactory',
    '$state',
    'propertiesServices',
    'toaster',
    'orchestratorService',
    'locationService',
    'appEnvironments',
    '$interval',
    'toscaService',
    'topologyJsonProcessor',
    'topoEditWf',
    'topoEditDisplay',
    function($scope,
      applicationServices,
      $translate,
      deploymentServices,
      applicationEventServicesFactory,
      $state,
      propertiesServices,
      toaster,
      orchestratorService,
      locationService,
      appEnvironments,
      $interval,
      toscaService,
      topologyJsonProcessor,
      topoEditWf,
      topoEditDisplay) {
      $scope.isRuntime = true;

      topoEditWf($scope);
      topoEditDisplay($scope);

      var pageStateId = $state.current.name;
      var applicationId = $state.params.id;
      $scope.selectedEnvironment = null;
      $scope.triggerTopologyRefresh = null;

      var updateSelectedEnvionment = function() {
        $scope.runtimeEnvironments = appEnvironments.deployEnvironments;
        //maybe the state request was made to open the view on a specific environment
        if(_.defined($state.params.openOnEnvironment) && appEnvironments.selected.id !== $state.params.openOnEnvironment){
         appEnvironments.select($state.params.openOnEnvironment, function(){
           if(appEnvironments.selected.status !== 'UNDEPLOYED'){
             $scope.selectedEnvironment = appEnvironments.selected;
           }
         });
       } else if (_.defined(appEnvironments.selected) && appEnvironments.selected.status !== 'UNDEPLOYED') {
           // select current environment
          $scope.selectedEnvironment = appEnvironments.selected;
        } else {
          //otherwise, just select the first deployed envionment
          for (var i = 0; i < appEnvironments.deployEnvironments.length && _.undefined($scope.selectedEnvironment); i++) {
            if (appEnvironments.deployEnvironments[i].status !== 'UNDEPLOYED') {
              $scope.selectedEnvironment = appEnvironments.deployEnvironments[i];
            }
            appEnvironments.select($scope.selectedEnvironment);
          }
        }
      };

      //update the selectedEnvironment
      updateSelectedEnvionment();

      // get the related cloud to display informations.
      var refreshOrchestratorInfo = function() {
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
      };

      $scope.eventTypeLabels = {
        'ALL': 'APPLICATIONS.RUNTIME.EVENTS.ALL',
        'paasdeploymentstatusmonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.STATUS',
        'paasinstancestatemonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.INSTANCES',
        'paasinstancepersistentresourcemonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.STORAGE',
        'paasmessagemonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.MESSAGES'
      };

      $scope.eventTypeFilters = [{
        'value': 'ALL'
      }, {
        'value': 'paasdeploymentstatusmonitorevent'
      }, {
        'value': 'paasinstancestatemonitorevent'
      }, {
        'value': 'paasinstancepersistentresourcemonitorevent'
      }, {
        'value': 'paasmessagemonitorevent'
      }];

      $scope.selectedEventTypeFilter = $scope.eventTypeFilters[0];
      $scope.filterEvents = function(filter) {
        $scope.selectedEventTypeFilter = filter;
      };

      var applicationEventServices = null;

      function getPAASEvents() {
        deploymentServices.getEvents({
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, function(result) {
          // display events
          if (_.undefined(result.data) || _.undefined(result.data.data)) {
            $scope.events = {};
            $scope.events.data = [];
          } else {
            for (var i = 0; i < result.data.data.length; i++) {
              var event = result.data.data[i];
              event.rawType = result.data.types[i];
              enrichPAASEvent(event);
            }
            $scope.events = result.data;
          }
          // if we already have a listener then stop it
          if (applicationEventServices !== null) {
            applicationEventServices.stop();
          }
          applicationEventServices = applicationEventServicesFactory(applicationId, $scope.selectedEnvironment.id);
          applicationEventServices.start();
          applicationEventServices.subscribe(pageStateId, onStatusChange);
        });
      }

      $scope.$on('$destroy', function() {
        if (applicationEventServices !== null) {
          applicationEventServices.stop();
        }
      });

      function enrichPAASEvent(event) {
        event.type = $scope.eventTypeLabels[event.rawType];
        switch (event.rawType) {
          case 'paasdeploymentstatusmonitorevent':
            event.message = {
              template: 'APPLICATIONS.RUNTIME.EVENTS.DEPLOYMENT_STATUS_MESSAGE',
              data: {
                status: 'DEPLOYMENT.STATUS.' + event.deploymentStatus
              }
            };
            break;
          case 'paasinstancestatemonitorevent':
            if (_.defined(event.instanceState)) {
              event.message = {
                template: 'APPLICATIONS.RUNTIME.EVENTS.INSTANCE_STATE_MESSAGE',
                data: {
                  state: event.instanceState,
                  nodeId: event.nodeTemplateId,
                  instanceId: event.instanceId
                }
              };
            } else {
              event.message = {
                template: 'APPLICATIONS.RUNTIME.EVENTS.INSTANCE_DELETED_MESSAGE',
                data: {
                  nodeId: event.nodeTemplateId,
                  instanceId: event.instanceId
                }
              };
            }
            break;
          case 'paasinstancepersistentresourcemonitorevent':
            event.message = {
              template: 'APPLICATIONS.RUNTIME.EVENTS.STORAGE_MESSAGE',
              data: {
                state: event.instanceState,
                nodeId: event.nodeTemplateId,
                instanceId: event.instanceId,
                volumeId: event.propertyValue
              }
            };
            break;
          case 'paasmessagemonitorevent':
            event.message = {
              template: event.message
            };
            break;
        }
      }

      function refreshSelectedNodeInstancesCount() {
        if (_.defined($scope.selectedNodeTemplate)) {
          if (_.defined($scope.topology.instances) && _.defined($scope.topology.instances[$scope.selectedNodeTemplate.name])) {
            $scope.selectedNodeTemplate.instancesCount = Object.keys($scope.topology.instances[$scope.selectedNodeTemplate.name]).length;
          } else {
            $scope.selectedNodeTemplate.instancesCount = 0;
          }
          if (_.undefined($scope.selectedNodeTemplate.newInstancesCount)) {
            $scope.selectedNodeTemplate.newInstancesCount = $scope.selectedNodeTemplate.instancesCount;
          }
        }
      }

      var refreshNodeInstanceInMaintenanceMode = function() {
        var hasNOdeInstanceInMaintenanceMode = false;
        if (_.defined($scope.topology.instances)) {
          angular.forEach($scope.topology.instances, function(v) {
            if (_.defined(v)) {
              angular.forEach(v, function(vv) {
                if (_.defined(vv) && vv.instanceStatus === 'MAINTENANCE') {
                  hasNOdeInstanceInMaintenanceMode = true;
                }
              });
            }
          });
        }
        $scope.hasNOdeInstanceInMaintenanceMode = hasNOdeInstanceInMaintenanceMode;
      };


      function refreshInstancesStatuses() {
        applicationServices.runtime.get({
          applicationId: applicationId,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, function(successResult) {
          if (!angular.equals($scope.topology.instances, successResult.data)) {
            getPAASEvents();
            $scope.topology.instances = successResult.data;
            refreshSelectedNodeInstancesCount();
            refreshNodeInstanceInMaintenanceMode();
            $scope.triggerTopologyRefresh = {};
          }
        });
      }

      /////////////////////////////////////////////////////////////
      // Initialize the view (we have to get the runtime topology)
      /////////////////////////////////////////////////////////////
      $scope.loadTopologyRuntime = function loadTopologyRuntime() {
        delete $scope.topology;
        deploymentServices.runtime.getTopology({
          applicationId: applicationId,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, function(successResult) { // get the topology
          $scope.topology = successResult.data;
          $scope.workflows.setCurrentWorkflowName('install');
          topologyJsonProcessor.process($scope.topology);
          refreshInstancesStatuses(); // update instance states
          refreshOrchestratorInfo(); // cloud info for deployment view
        });
      };

      var onStatusChange = function(type, event) {
        // Enrich the event with the type based on the topic destination
        event.rawType = type;
        enrichPAASEvent(event);
        $scope.events.data.push(event);
        if (!$scope.isWaitingForRefresh) {
          $scope.isWaitingForRefresh = true;
          $interval(function() {
            $scope.isWaitingForRefresh = false;
            refreshInstancesStatuses();
          }, 1000, 1);
          refreshNodeInstanceInMaintenanceMode();
          $scope.$digest();
        }
      };

      $scope.$on('$destroy', function() {
        // UnSubscribe
        applicationEventServices.unsubscribe(pageStateId);
      });


      var injectPropertyDefinitionToInterfaces = function(interfaces) {

        if (_.defined(interfaces)) {
          angular.forEach(interfaces, function(interfaceObj) {
            Object.keys(interfaceObj.operations).forEach(function(operation) {
              if (_.defined(interfaceObj.operations[operation].inputParameters)) {
                Object.keys(interfaceObj.operations[operation].inputParameters).forEach(function(paramName) {
                  var inputParameter = interfaceObj.operations[operation].inputParameters[paramName];
                  if (inputParameter.definition) {
                    var propteryDefinitionModel = {};
                    propteryDefinitionModel.type = inputParameter.type;
                    propteryDefinitionModel.required = inputParameter.required;
                    propteryDefinitionModel.name = paramName;
                    propteryDefinitionModel.default = inputParameter.paramValue || ''; // needed for the directive
                    propteryDefinitionModel.password = false;
                    propteryDefinitionModel.constraints = null;
                    propteryDefinitionModel.from = operation;
                    if (inputParameter.type === 'boolean') {
                      inputParameter.paramValue = false;
                    }
                    if (inputParameter.type === 'timestamp') {
                      inputParameter.paramValue = Date.now();
                    }
                    inputParameter.definitionModel = propteryDefinitionModel;
                  } else {
                    //we don't want function type params in the ui
                    delete interfaceObj.operations[operation].inputParameters[paramName];
                  }
                });
              }
            });
          });

        }

      };

      $scope.checkProperty = function(definition, value, interfaceName) {
        var checkPropertyRequest = {
          'definitionId': definition.name,
          'propertyDefinition': definition,
          'value': value
        };

        return propertiesServices.validConstraints({}, angular.toJson(checkPropertyRequest), function(successResult) {
          if (successResult.error !== null) {
            // Possible errors
            // 800 : constraint error in a property definition
            // 804 : type constraint for a property definition
            // Constraint error display + translation
            var constraintInfo = successResult.data;
            var errorMessage = null;
            if (successResult.error.code === 804) {
              errorMessage = $translate.instant('ERRORS.' + successResult.error.code, constraintInfo);
            } else { // 800
              errorMessage = $translate.instant('ERRORS.' + successResult.error.code + '.' + constraintInfo.name, constraintInfo);
            }
          } else {
            // No errors
            $scope.selectedNodeCustomInterfaces[interfaceName].operations[definition.from].inputParameters[definition.name].paramValue = value;
          }
        }).$promise;

      };

      $scope.selectNodeTemplate = function(newSelectedName, oldSelectedName) {
        var oldSelected = $scope.topology.topology.nodeTemplates[oldSelectedName] || $scope.selectedNodeTemplate;
        if (oldSelected) {
          oldSelected.selected = false;
        }

        var newSelected = $scope.topology.topology.nodeTemplates[newSelectedName];
        newSelected.selected = true;

        $scope.selectedNodeTemplate = newSelected;
        $scope.triggerTopologyRefresh = {};
        $scope.selectedNodeTemplate.name = newSelectedName;
        if ($scope.isComputeType($scope.selectedNodeTemplate)) {
          $scope.selectedNodeTemplate.scalingPolicy = toscaService.getScalingPolicy($scope.selectedNodeTemplate);
        }
        // custom interface if exists
        var nodetype = $scope.topology.nodeTypes[$scope.selectedNodeTemplate.type];
        delete $scope.selectedNodeCustomInterfaces;
        if (nodetype.interfaces) {
          $scope.selectedNodeCustomInterfaces = {};
          angular.forEach(nodetype.interfaces, function(interfaceObj, interfaceName) {
            if (interfaceName !== toscaService.standardInterfaceName) {
              $scope.selectedNodeCustomInterfaces[interfaceName] = interfaceObj;
            }
          });
          angular.forEach($scope.selectedNodeTemplate.interfaces, function(interfaceObj, interfaceName) {
            if (interfaceName !== toscaService.standardInterfaceName) {
              $scope.selectedNodeCustomInterfaces[interfaceName] = interfaceObj;
            }
          });
          if (_.isNotEmpty($scope.selectedNodeCustomInterfaces)) {
            // create and inject property definition in order to use <property-display> directive for input parameters
            injectPropertyDefinitionToInterfaces($scope.selectedNodeCustomInterfaces);
          } else {
            delete $scope.selectedNodeCustomInterfaces;
          }
        }
        refreshSelectedNodeInstancesCount();
        $scope.clearInstanceSelection();
        $scope.display.set('details', true);
        $scope.$apply();
      };

      $scope.selectInstance = function(id) {
        $scope.selectedInstance = $scope.topology.instances[$scope.selectedNodeTemplate.name][id];
        $scope.selectedInstance.id = id;
      };

      $scope.clearInstanceSelection = function() {
        delete $scope.selectedInstance;
      };

      $scope.clearNodeSelection = function() {
        $scope.clearInstanceSelection();
        delete $scope.selectedNodeTemplate;
      };

      $scope.isScalable = function() {
        return $scope.selectedNodeTemplate && $scope.selectedNodeTemplate.scalingPolicy;
      };

      $scope.scale = function(newValue) {
        if (newValue !== $scope.selectedNodeTemplate.instancesCount) {
          applicationServices.scale({
            applicationId: applicationId,
            nodeTemplateId: $scope.selectedNodeTemplate.name,
            instances: (newValue - $scope.selectedNodeTemplate.instancesCount),
            applicationEnvironmentId: $scope.selectedEnvironment.id
          }, undefined, function success() {
            $scope.loadTopologyRuntime();
          });
        }
      };

      $scope.launchWorkflow = function() {
        $scope.isLaunchingWorkflow = true;
        applicationServices.launchWorkflow({
          applicationId: applicationId,
          applicationEnvironmentId: $scope.selectedEnvironment.id,
          workflowName: $scope.currentWorkflowName
        }, undefined, function success() {
          $scope.isLaunchingWorkflow = false;
        });
      };

      $scope.filter = null;

      /** EXECUTE OPERATIONS */
      $scope.isMapNotNullOrEmpty = _.isNotEmpty;

      $scope.executeOperation = function(interfaceName, operationName, params, event) {
        if (event) {
          event.stopPropagation();
        }
        var instanceId = $scope.selectedInstance ? $scope.selectedInstance.id : null;

        // prepare parameters and operationParamDefinitions
        var preparedParams = {};
        if (params !== null) {
          Object.keys(params).forEach(function(param) {
            preparedParams[params[param].definitionModel.name] = params[param].paramValue;
          });
        }
        // generate the request object
        var operationExecRequest = {
          applicationEnvironmentId: $scope.selectedEnvironment.id,
          nodeTemplateName: $scope.selectedNodeTemplate.name,
          instanceId: instanceId,
          interfaceName: interfaceName,
          operationName: operationName,
          parameters: preparedParams
        };

        $scope.operationLoading = $scope.operationLoading || {};
        $scope.operationLoading[$scope.selectedNodeTemplate.name] = $scope.operationLoading[$scope.selectedNodeTemplate.name] || {};
        $scope.operationLoading[$scope.selectedNodeTemplate.name][interfaceName] = $scope.operationLoading[$scope.selectedNodeTemplate.name][interfaceName] || {};
        $scope.operationLoading[$scope.selectedNodeTemplate.name][interfaceName][operationName] = true;

        deploymentServices.runtime.executeOperation({
          applicationId: applicationId
        }, angular.toJson(operationExecRequest), function(successResult) {
          // success
          $scope.operationLoading[$scope.selectedNodeTemplate.name][interfaceName][operationName] = false;
          if (successResult.error !== null) {
            var title = $translate.instant('ERRORS.' + successResult.error.code + '.TITLE', {
              'operation': operationName
            });
            var message = null;
            // Possible errors
            // 800 : constraint error in a property definition
            // 804 : type constraint for a property definition
            // 805 : required constraint for a property definition
            // 371 : Operation exception
            if (successResult.error.code === 804 || successResult.error.code === 805) { // Type matching error
              message = $translate.instant('ERRORS.' + successResult.error.code + '.MESSAGE', successResult.data);
            } else if (successResult.error.code === 800) { // Constraint error
              var constraintInfo = successResult.data;
              message = $translate.instant('ERRORS.' + successResult.error.code + '.' + constraintInfo.name, constraintInfo);
            } else { // code 371, execution error
              message = successResult.error.message;
            }
            toaster.pop('error', title, message, 6000, 'trustedHtml', null);

          } else if (!_.undefined(successResult.data)) {
            var successTitle = $translate.instant('APPLICATIONS.RUNTIME.OPERATION_EXECUTION.RESULT_TITLE', {
              'operation': operationName
            });
            // Toaster HTML result preview for all instances
            var resultInstanceMap = successResult.data;
            var resultHtml = [];
            resultHtml.push('<ul>');
            Object.keys(resultInstanceMap).forEach(function(instanceId) {
              if (resultInstanceMap[instanceId]) {
                resultHtml.push('<li>Instance ' + instanceId + ' : ' + resultInstanceMap[instanceId] + '</li>');
              } else {
                resultHtml.push('<li>Instance ' + instanceId + ' : OK (' + $translate.instant('APPLICATIONS.RUNTIME.OPERATION_EXECUTION.NO_RETURN') + ')</li>');
              }

            });
            resultHtml.push('</ul>');
            toaster.pop('success', successTitle, resultHtml.join(''), 4000, 'trustedHtml', null);
          }

        }, function(errorResult) {
          console.error('executeOperation ERROR', errorResult);
          $scope.operationLoading[$scope.selectedNodeTemplate.name][interfaceName][operationName] = false;
        });

        // reset parameter inputs ?
        injectPropertyDefinitionToInterfaces($scope.selectedNodeCustomInterface);
      };

      // check if compute type
      $scope.isComputeType = function(nodeTemplate) {
        if (_.undefined($scope.topology) || _.undefined(nodeTemplate)) {
          return false;
        }
        return toscaService.isComputeType(nodeTemplate.type, $scope.topology.nodeTypes);
      };

      $scope.switchNodeInstanceMaintenanceMode = function(nodeInstanceId) {
        switch ($scope.topology.instances[$scope.selectedNodeTemplate.name][nodeInstanceId].instanceStatus) {
        case 'SUCCESS':
          deploymentServices.nodeInstanceMaintenanceOn({
            applicationId: applicationId,
            applicationEnvironmentId: $scope.selectedEnvironment.id,
            nodeTemplateId: $scope.selectedNodeTemplate.name,
            instanceId: nodeInstanceId
          }, {}, undefined);
          break;
        case 'MAINTENANCE':
          deploymentServices.nodeInstanceMaintenanceOff({
            applicationId: applicationId,
            applicationEnvironmentId: $scope.selectedEnvironment.id,
            nodeTemplateId: $scope.selectedNodeTemplate.name,
            instanceId: nodeInstanceId
          }, {}, undefined);
        }
      };

      $scope.switchDeployementMaintenanceMode = function() {
        if ($scope.hasNOdeInstanceInMaintenanceMode) {
          deploymentServices.deploymentMaintenance.off({
            applicationId: applicationId,
            applicationEnvironmentId: $scope.selectedEnvironment.id
          }, {}, undefined);
        } else {
          deploymentServices.deploymentMaintenance.on({
            applicationId: applicationId,
            applicationEnvironmentId: $scope.selectedEnvironment.id
          }, {}, undefined);
        }
      };

      $scope.changeEnvironment = function(selectedEnvironment) {
        appEnvironments.select(selectedEnvironment.id, function() {
          $scope.selectedEnvironment = appEnvironments.selected;
          // update the environment
          $scope.loadTopologyRuntime();
          $scope.clearNodeSelection();
        });
      };

      // first topology load
      $scope.loadTopologyRuntime();
      $scope.view = 'RENDERED';
    }
  ]);
});
