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

  states.state('applications.detail.environment.deploycurrent.runtimeeditor', {
    url: '/runtime_editor',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_runtimeeditor.html',
    controller: 'ApplicationEnvDeployCurrentRuntimeEditorCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.runtimeeditor',
      state: 'applications.detail.environment.deploycurrent.runtimeeditor',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.RUNTIME_EDITOR',
      icon: 'fa fa-play',
      priority: 200
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentRuntimeEditorCtrl',
  ['$scope',
  'applicationServices',
  '$translate',
  'deploymentServices',
  'propertiesServices',
  'toaster',
  '$interval',
  'toscaService',
  'topoEditDisplay',
  'breadcrumbsService',
  '$state',
  function($scope,
    applicationServices,
    $translate,
    deploymentServices,
    propertiesServices,
    toaster,
    $interval,
    toscaService,
    topoEditDisplay,
    breadcrumbsService,
    $state) {

    breadcrumbsService.putConfig({
      state : 'applications.detail.environment.deploycurrent.runtimeeditor',
      text: function(){
        return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.RUNTIME_EDITOR');
      },
      onClick: function(){
        $state.go('applications.detail.environment.deploycurrent.runtimeeditor');
      }
    });

    $scope.isRuntime = true;

    $scope.displays = {
      details: { active: true, size: 500, selector: '#runtime-details-box', only: ['topology', 'details'], title: 'DETAILS', fa: 'fa-info' },
      events: { active: false, size: 500, selector: '#runtime-events-box', only: ['topology', 'events'], title: 'APPLICATIONS.RUNTIME.EVENTS_TAB', fa: 'fa-tasks' },
      service: { active: false, size: 500, selector: '#runtime-service-box', only: ['topology', 'service'], title: 'SERVICES.MANAGED.TITLE', fa: 'fa-globe' }
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
      var hasNodeInstanceInMaintenanceMode = false;
      if (_.defined($scope.topology.instances)) {
        angular.forEach($scope.topology.instances, function(v) {
          if (_.defined(v)) {
            angular.forEach(v, function(vv) {
              if (_.defined(vv) && vv.instanceStatus === 'MAINTENANCE') {
                hasNodeInstanceInMaintenanceMode = true;
              }
            });
          }
        });
      }
      $scope.hasNodeInstanceInMaintenanceMode = hasNodeInstanceInMaintenanceMode;
    };


    function refreshInstancesStatuses() {
      applicationServices.runtime.get({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.environment.id
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
        angular.forEach(interfaces, function(interfaceObj, interfaceName) {
          Object.keys(interfaceObj.operations).forEach(function(operation) {
            if (_.defined(interfaceObj.operations[operation].inputParameters)) {
              Object.keys(interfaceObj.operations[operation].inputParameters).forEach(function(paramName) {
                var inputParameter = interfaceObj.operations[operation].inputParameters[paramName];
                if (inputParameter.definition) {
                  inputParameter.interface = interfaceName;
                  inputParameter.operation = operation;
                  inputParameter.paramValue = inputParameter.paramValue || _.get(inputParameter, 'default.value') || '';
                  // if (inputParameter.type === 'timestamp') {
                  //   inputParameter.paramValue = Date.now();
                  // }
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

    $scope.checkProperty = function(definition, value, propertyName) {
      var checkPropertyRequest = {
        'definitionId': definition.name,
        'propertyDefinition': definition,
        'value': value
      };

      return propertiesServices.validConstraints({}, angular.toJson(checkPropertyRequest), function(successResult) {
        if (_.undefined(successResult.error)) {
          // No errors
          $scope.selectedNodeCustomInterfaces[definition.interface].operations[definition.operation].inputParameters[propertyName].paramValue = value;
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
          applicationEnvironmentId: $scope.environment.id
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
        _.each(params, function(param, name){
          preparedParams[name] = param.paramValue;
        });
      }
      // generate the request object
      var operationExecRequest = {
        applicationEnvironmentId: $scope.environment.id,
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
          applicationEnvironmentId: $scope.environment.id,
          nodeTemplateId: $scope.selectedNodeTemplate.name,
          instanceId: nodeInstanceId
        }, {}, undefined);
        break;
      case 'MAINTENANCE':
        deploymentServices.nodeInstanceMaintenanceOff({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.environment.id,
          nodeTemplateId: $scope.selectedNodeTemplate.name,
          instanceId: nodeInstanceId
        }, {}, undefined);
      }
    };

    $scope.switchDeployementMaintenanceMode = function() {
      if ($scope.hasNodeInstanceInMaintenanceMode) {
        deploymentServices.deploymentMaintenance.off({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.environment.id
        }, {}, undefined);
      } else {
        deploymentServices.deploymentMaintenance.on({
          applicationId: $scope.application.id,
          applicationEnvironmentId: $scope.environment.id
        }, {}, undefined);
      }
    };

    /**
    * First load. This is necessary if we come on the runtime view state once the application is fully deployed
    **/
    var firstLoad = function () {
      if(_.defined($scope.topology) && _.undefined($scope.topology.instances)){
        refreshInstancesStatuses();
        refreshNodeInstanceInMaintenanceMode();
      }
    };

    firstLoad();
  }
]);
});
