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

  states.state('applications.detail.runtime.topology', {
    url: '/topology',
    templateUrl: 'views/topology/topology_runtime.html',
    controller: 'a4cRuntimeTopologyCtrl',
    menu: {
      id: 'am.applications.detail.runtime.topology',
      state: 'applications.detail.runtime.topology',
      key: 'EDITOR.MENU_TOPOLOGY',
      icon: 'fa fa-sitemap',
      priority: 1
    }
  });
  states.forward('applications.detail.runtime', 'applications.detail.runtime.topology');

  modules.get('a4c-applications').controller('a4cRuntimeTopologyCtrl',
    ['$scope',
    'applicationServices',
    '$translate',
    'deploymentServices',
    'propertiesServices',
    'toaster',
    '$interval',
    'toscaService',
    'topoEditDisplay',
    function($scope,
      applicationServices,
      $translate,
      deploymentServices,
      propertiesServices,
      toaster,
      $interval,
      toscaService,
      topoEditDisplay) {
      $scope.isRuntime = true;

      $scope.displays = {
        details: { active: true, size: 500, selector: '#runtime-details-box', only: ['topology', 'details'] },
        events: { active: false, size: 500, selector: '#runtime-events-box', only: ['topology', 'events'] }
      };
      topoEditDisplay($scope, '#topology-editor');
      $scope.view = 'RENDERED';
      $scope.triggerTopologyRefresh = null;

      $scope.eventTypeFilters = [
        { 'value': 'ALL' },
        { 'value': 'paasdeploymentstatusmonitorevent' },
        { 'value': 'paasinstancestatemonitorevent' },
        { 'value': 'paasinstancepersistentresourcemonitorevent' },
        { 'value': 'paasmessagemonitorevent' }
      ];

      $scope.selectedEventTypeFilter = $scope.eventTypeFilters[0];
      $scope.filterEvents = function(filter) {
        $scope.selectedEventTypeFilter = filter;
      };


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
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.selectedEnvironment.id
        }, function(successResult) {
          if (!angular.equals($scope.topology.instances, successResult.data)) {
            $scope.topology.instances = successResult.data;
            refreshSelectedNodeInstancesCount();
            refreshNodeInstanceInMaintenanceMode();
            $scope.triggerTopologyRefresh = {};
          }
        });
      }

      $scope.$on('a4cRuntimeTopologyLoaded', function() {
        refreshInstancesStatuses();
        refreshNodeInstanceInMaintenanceMode();
      });

      $scope.$on('a4cRuntimeEventReceived', function(angularEvent, event) {
        if(event.rawType === 'paasmessagemonitorevent') {
          return;
        }
        // topology has changed
        if (!$scope.isWaitingForRefresh) {
          $scope.isWaitingForRefresh = true;
          $interval(function() {
            $scope.isWaitingForRefresh = false;
            refreshInstancesStatuses();
          }, 1000, 1);
          refreshNodeInstanceInMaintenanceMode();
          $scope.$digest();
        }
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
        if ($scope.isComputeType($scope.selectedNodeTemplate) || isDockerType($scope.selectedNodeTemplate)) {
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
            applicationId: $scope.application.id,
            nodeTemplateId: $scope.selectedNodeTemplate.name,
            instances: (newValue - $scope.selectedNodeTemplate.instancesCount),
            applicationEnvironmentId: $scope.selectedEnvironment.id
          }, undefined, function success() {
            $scope.loadTopologyRuntime();
          });
        }
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
          applicationId: $scope.application.id
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

      // check if docker type
      function isDockerType(nodeTemplate) {
        if (_.undefined($scope.topology) || _.undefined(nodeTemplate)) {
          return false;
        }
        return toscaService.isDockerType(nodeTemplate.type, $scope.topology.nodeTypes);
      }

      $scope.switchNodeInstanceMaintenanceMode = function(nodeInstanceId) {
        switch ($scope.topology.instances[$scope.selectedNodeTemplate.name][nodeInstanceId].instanceStatus) {
        case 'SUCCESS':
          deploymentServices.nodeInstanceMaintenanceOn({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.selectedEnvironment.id,
            nodeTemplateId: $scope.selectedNodeTemplate.name,
            instanceId: nodeInstanceId
          }, {}, undefined);
          break;
        case 'MAINTENANCE':
          deploymentServices.nodeInstanceMaintenanceOff({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.selectedEnvironment.id,
            nodeTemplateId: $scope.selectedNodeTemplate.name,
            instanceId: nodeInstanceId
          }, {}, undefined);
        }
      };

      $scope.switchDeployementMaintenanceMode = function() {
        if ($scope.hasNOdeInstanceInMaintenanceMode) {
          deploymentServices.deploymentMaintenance.off({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.selectedEnvironment.id
          }, {}, undefined);
        } else {
          deploymentServices.deploymentMaintenance.on({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.selectedEnvironment.id
          }, {}, undefined);
        }
      };
    }
  ]);
});
