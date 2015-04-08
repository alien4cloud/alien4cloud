/* global UTILS, CONSTANTS */

'use strict';

angular.module('alienUiApp').controller(
  'TopologyRuntimeCtrl', ['$scope', 'applicationServices', '$translate', 'resizeServices', 'deploymentServices', 'applicationEventServicesFactory', '$state', 'propertiesServices', 'toaster', 'cloudServices', 'applicationEnvironmentServices', 'appEnvironments',
    function($scope, applicationServices, $translate, resizeServices, deploymentServices, applicationEventServicesFactory, $state, propertiesServices, toaster, cloudServices, applicationEnvironmentServices, appEnvironments) {
      var pageStateId = $state.current.name;
      var applicationId = $state.params.id;

      $scope.runtimeEnvironments = appEnvironments.deployEnvironments;
      // select current environment
      if (UTILS.isDefinedAndNotNull(appEnvironments.selectedEnvironment) && appEnvironments.selectedEnvironment.status !== 'UNDEPLOYED') {
        $scope.selectedEnvironment = appEnvironments.selectedEnvironment;
      } else {
        $scope.selectedEnvironment = null;
        for (var i = 0; i < appEnvironments.deployEnvironments.length && $scope.selectedEnvironment === null; i++) {
          if (appEnvironments.deployEnvironments[i].status !== 'UNDEPLOYED') {
            $scope.selectedEnvironment = appEnvironments.deployEnvironments[i];
          }
          appEnvironments.selectedEnvironment = $scope.selectedEnvironment;
        }
      }

      // get the related cloud to display informations.
      var refreshCloudInfo = function() {
        cloudServices.get({id: $scope.selectedEnvironment.cloudId}, function(response) {
          $scope.cloud = response.data.cloud;
        });
      };
      refreshCloudInfo();

      var CUSTOM_INTERFACE_NAME = 'custom';
      $scope.eventTypeLabels = {
        'ALL': 'APPLICATIONS.RUNTIME.EVENTS.ALL',
        'paasdeploymentstatusmonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.STATUS',
        'paasinstancestatemonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.INSTANCES',
        'paasinstancestoragemonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.STORAGE',
        'paasmessagemonitorevent': 'APPLICATIONS.RUNTIME.EVENTS.MESSAGES'
      };

      $scope.eventTypeFilters = [{'value': 'ALL'},
        {'value': 'paasdeploymentstatusmonitorevent'},
        {'value': 'paasinstancestatemonitorevent'},
        {'value': 'paasinstancestoragemonitorevent'},
        {'value': 'paasmessagemonitorevent'}];

      $scope.selectedEventTypeFilter = $scope.eventTypeFilters[0];
      $scope.filterEvents = function(filter) {
        $scope.selectedEventTypeFilter = filter;
      };

      // Layout resize
      var borderSpacing = 10;
      var border = 2;
      var detailDivWidth = 450;
      var widthOffset = detailDivWidth + (3 * borderSpacing) + (2 * border);

      function onResize(width, height) {
        $scope.dimensions = {
          width: width,
          height: height
        };

        $scope.eventsDivHeight = resizeServices.getHeight(236);
      }

      resizeServices.register(onResize, widthOffset, 124);

      $scope.dimensions = {
        height: resizeServices.getHeight(124),
        width: resizeServices.getWidth(widthOffset)
      };
      $scope.eventsDivHeight = resizeServices.getHeight(236);
      // End Layout resize

      var applicationEventServices = null;

      function getPAASEvents() {
        deploymentServices.getEvents({
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, function(result) {
          // display events
          if (UTILS.isUndefinedOrNull(result.data) || UTILS.isUndefinedOrNull(result.data.data)) {
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
                status: 'APPLICATIONS.' + event.deploymentStatus
              }
            };
            break;
          case 'paasinstancestatemonitorevent':
            if (UTILS.isDefinedAndNotNull(event.instanceState)) {
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
          case 'paasinstancestoragemonitorevent':
            if (UTILS.isDefinedAndNotNull(event.instanceState)) {
              event.message = {
                template: 'APPLICATIONS.RUNTIME.EVENTS.STORAGE_MESSAGE',
                data: {
                  state: event.instanceState,
                  nodeId: event.nodeTemplateId,
                  instanceId: event.instanceId,
                  volumeId: event.volumeId
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
          case 'paasmessagemonitorevent':
            event.message = {
              template: event.message
            };
            break;
        }
      }

      function refreshSelectedNodeInstancesCount() {
        if (UTILS.isDefinedAndNotNull($scope.selectedNodeTemplate)) {
          if (UTILS.isDefinedAndNotNull($scope.topology.instances) && UTILS.isDefinedAndNotNull($scope.topology.instances[$scope.selectedNodeTemplate.name])) {
            $scope.selectedNodeTemplate.instancesCount = Object.keys($scope.topology.instances[$scope.selectedNodeTemplate.name]).length;
          } else {
            $scope.selectedNodeTemplate.instancesCount = 0;
          }
          if (UTILS.isUndefinedOrNull($scope.selectedNodeTemplate.newInstancesCount)) {
            $scope.selectedNodeTemplate.newInstancesCount = $scope.selectedNodeTemplate.instancesCount;
          }
        }
      }

      var refreshNodeInstanceInMaintenanceMode = function() {
        var hasNOdeInstanceInMaintenanceMode = false;
        if (UTILS.isDefinedAndNotNull($scope.topology.instances)) {
          angular.forEach($scope.topology.instances, function (v, k) {
            if (UTILS.isDefinedAndNotNull(v)) {
              angular.forEach(v, function (vv, kk) {
                if (UTILS.isDefinedAndNotNull(vv) && vv.instanceStatus === 'MAINTENANCE') {
                  hasNOdeInstanceInMaintenanceMode = true;
                }
              });
            }
          });
        }
        $scope.hasNOdeInstanceInMaintenanceMode = hasNOdeInstanceInMaintenanceMode;
      }
      
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
          refreshInstancesStatuses(); // update instance states
          refreshCloudInfo(); // cloud info for deployment view
        });
      };

      var onStatusChange = function(type, event) {
        // Enrich the event with the type based on the topic destination
        event.rawType = type;
        enrichPAASEvent(event);
        $scope.events.data.push(event);
        if (UTILS.isUndefinedOrNull(event.instanceState)) {
          // Delete event
          if ($scope.topology.instances.hasOwnProperty(event.nodeTemplateId)) {
            if ($scope.topology.instances[event.nodeTemplateId].hasOwnProperty(event.instanceId)) {
              delete $scope.topology.instances[event.nodeTemplateId][event.instanceId];
              if (Object.keys($scope.topology.instances[event.nodeTemplateId]).length === 0) {
                delete $scope.topology.instances[event.nodeTemplateId];
              }
            }
          }
        } else {
          // Add or modify event
          if (!$scope.topology.instances.hasOwnProperty(event.nodeTemplateId)) {
            $scope.topology.instances[event.nodeTemplateId] = {};
          }
          if (!$scope.topology.instances[event.nodeTemplateId].hasOwnProperty(event.instanceId)) {
            $scope.topology.instances[event.nodeTemplateId][event.instanceId] = {};
          }
          $scope.topology.instances[event.nodeTemplateId][event.instanceId].state = event.instanceState;
          $scope.topology.instances[event.nodeTemplateId][event.instanceId].instanceStatus = event.instanceStatus;
          $scope.topology.instances[event.nodeTemplateId][event.instanceId].runtimeProperties = event.runtimeProperties;
          $scope.topology.instances[event.nodeTemplateId][event.instanceId].attributes = event.attributes;
        }
        refreshSelectedNodeInstancesCount();
        refreshNodeInstanceInMaintenanceMode();
        $scope.$apply();
      };

      $scope.$on('$destroy', function() {
        // UnSubscribe
        applicationEventServices.unsubscribe(pageStateId);
      });


      var injectPropertyDefinitionToInterfaces = function(interfaces) {

        if (UTILS.isDefinedAndNotNull(interfaces)) {

          var propteryDefinitionModel = {};
          var inputParameter = {};
          Object.keys(interfaces.operations).forEach(function(operation) {
            if (UTILS.isDefinedAndNotNull(interfaces.operations[operation].inputParameters)) {
              Object.keys(interfaces.operations[operation].inputParameters).forEach(function(paramName) {
                inputParameter = interfaces.operations[operation].inputParameters[paramName];
                if (inputParameter.definition) {
                  propteryDefinitionModel = {};
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
                  delete interfaces.operations[operation].inputParameters[paramName];
                }
              });
            }
          });

        }

      };

      $scope.checkProperty = function(definition, value) {

        var checkPropertyRequest = {
          'propertyId': definition.name,
          'propertyDefinition': definition,
          'propertyValue': value
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
              errorMessage = $translate('ERRORS.' + successResult.error.code, constraintInfo);
            } else { // 800
              errorMessage = $translate('ERRORS.' + successResult.error.code + '.' + constraintInfo.name, constraintInfo);
            }
          } else {
            // No errors
            $scope.selectedNodeCustomInterface.operations[definition.from].inputParameters[definition.name].paramValue = value;
          }
        }).$promise;

      };

      $scope.selectNodeTemplate = function(newSelectedName, oldSelectedName) {
        if (oldSelectedName) {
          var oldSelected = $scope.topology.topology.nodeTemplates[oldSelectedName];
          if (oldSelected) {
            oldSelected.selected = false;
          }
        }

        var newSelected = $scope.topology.topology.nodeTemplates[newSelectedName];
        newSelected.selected = true;

        $scope.selectedNodeTemplate = newSelected;
        $scope.selectedNodeTemplate.name = newSelectedName;
        // custom interface if exists
        var nodetype = $scope.topology.nodeTypes[$scope.selectedNodeTemplate.type];
        delete $scope.selectedNodeCustomInterface;
        if (nodetype.interfaces) {
          $scope.selectedNodeCustomInterface = nodetype.interfaces[CUSTOM_INTERFACE_NAME];
          // create and inject property definition in order to use <property-display> directive for input parameters
          injectPropertyDefinitionToInterfaces($scope.selectedNodeCustomInterface);
        }
        refreshSelectedNodeInstancesCount();
        $scope.clearInstanceSelection();
        $scope.$apply();
        document.getElementById('details-tab').click();
      };

      $scope.selectInstance = function(id) {
        $scope.selectedInstance = $scope.topology.instances[$scope.selectedNodeTemplate.name][id];
        $scope.selectedInstance.id = id;
      };

      $scope.clearInstanceSelection = function() {
        delete $scope.selectedInstance;
      };

      $scope.scale = function(newValue) {
        console.log('SCALA to >', newValue, $scope.topology);
        if (newValue !== $scope.selectedNodeTemplate.instancesCount) {
          applicationServices.scale({
            applicationId: applicationId,
            nodeTemplateId: $scope.selectedNodeTemplate.name,
            instances: (newValue - $scope.selectedNodeTemplate.instancesCount),
            applicationEnvironmentId: $scope.selectedEnvironment.id
          }, undefined, function success (scaleResult) {
            console.log('SCALE SUCCESS >', scaleResult);
            $scope.loadTopologyRuntime();
          });
        }
      };

      $scope.filter = null;

      /** EXECUTE OPERATIONS */
      $scope.isMapNotNullOrEmpty = UTILS.isMapNotNullOrEmpty;

      $scope.executeOperation = function(operationName, params, event) {
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
          interfaceName: CUSTOM_INTERFACE_NAME,
          operationName: operationName,
          parameters: preparedParams
        };

        $scope.operationLoading = $scope.operationLoading || {};
        $scope.operationLoading[$scope.selectedNodeTemplate.name] = $scope.operationLoading[$scope.selectedNodeTemplate.name] || {};
        $scope.operationLoading[$scope.selectedNodeTemplate.name][operationName] = true;

        deploymentServices.runtime.executeOperation({
          applicationId: applicationId
        }, angular.toJson(operationExecRequest), function(successResult) {
          // success
          $scope.operationLoading[$scope.selectedNodeTemplate.name][operationName] = false;
          if (successResult.error !== null) {
            var title = $translate('ERRORS.' + successResult.error.code + '.TITLE', {
              'operation': operationName
            });
            var message = null;
            // Possible errors
            // 800 : constraint error in a property definition
            // 804 : type constraint for a property definition
            // 805 : required constraint for a property definition
            // 371 : Operation exception
            if (successResult.error.code === 804 || successResult.error.code === 805) { // Type matching error
              message = $translate('ERRORS.' + successResult.error.code + '.MESSAGE', successResult.data);
            } else if (successResult.error.code === 800) { // Constraint error
              var constraintInfo = successResult.data;
              message = $translate('ERRORS.' + successResult.error.code + '.' + constraintInfo.name, constraintInfo);
            } else { // code 371, execution error
              message = successResult.error.message;
            }
            // to dismiss by the user
            toaster.pop('error', title, message, 0, 'trustedHtml', null);

          } else if (!UTILS.isUndefinedOrNull(successResult.data)) {

            title = $translate('APPLICATIONS.RUNTIME.OPERATION_EXECUTION.RESULT_TITLE', {
              'operation': operationName
            });
            // Toaster HTML result preview for all instances
            var resultInstanceMap = successResult.data;
            var resultHtml = [];
            resultHtml.push('<ul>');
            Object.keys(resultInstanceMap).forEach(function(instanceId) {
              resultHtml.push('<li>Instance ' + instanceId + ' : ' + resultInstanceMap[instanceId] + '</li>');
            });
            resultHtml.push('</ul>');
            // timeout at 0 == keep displayed until close
            toaster.pop('success', title, resultHtml.join(''), 0, 'trustedHtml', null);
          }

        }, function(errorResult) {
          console.error('executeOperation ERROR', errorResult);
          $scope.operationLoading[$scope.selectedNodeTemplate.name][operationName] = false;
        });

        // reset parameter inputs ?
        injectPropertyDefinitionToInterfaces($scope.selectedNodeCustomInterface);
      };

      // check if compute type
      $scope.isComputeType = function(nodeTemplate) {
        if (UTILS.isUndefinedOrNull($scope.topology) || UTILS.isUndefinedOrNull(nodeTemplate)) {
          return false;
        }
        var nodeType = $scope.topology.nodeTypes[nodeTemplate.type];
        return UTILS.isFromNodeType(nodeType, CONSTANTS.toscaComputeType);
      };

      $scope.switchNodeInstanceMaintenanceModeOn = function(nodeInstanceId) {
        deploymentServices.nodeInstanceMaintenanceOn({
          applicationId: applicationId,
          applicationEnvironmentId: $scope.selectedEnvironment.id,
          nodeTemplateId: $scope.selectedNodeTemplate.name,
          instanceId: nodeInstanceId
        }, {}, function(response) {
        });
      };
      $scope.switchNodeInstanceMaintenanceModeOff = function(nodeInstanceId) {
        deploymentServices.nodeInstanceMaintenanceOff({
          applicationId: applicationId,
          applicationEnvironmentId: $scope.selectedEnvironment.id,
          nodeTemplateId: $scope.selectedNodeTemplate.name,
          instanceId: nodeInstanceId
        }, {}, function(response) {
        });
      };   
      $scope.switchDeployementMaintenanceModeOn = function() {
        deploymentServices.deploymentMaintenance.on({
          applicationId: applicationId,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, {}, function(response) {
        });
      };
      $scope.switchDeployementMaintenanceModeOff = function() {
        deploymentServices.deploymentMaintenance.off({
          applicationId: applicationId,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, {}, function(response) {
        });
      };       
      
      // first topology load
      $scope.loadTopologyRuntime();
    }
  ]);
