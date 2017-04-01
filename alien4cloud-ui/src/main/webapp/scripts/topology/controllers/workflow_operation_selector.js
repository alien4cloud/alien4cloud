define(function (require) {
  'use strict';

  var modules = require('modules');
  require('angular-ui-ace');

  require('angular-tree-control');

  modules.get('a4c-topology-editor').controller('WfOperationSelectorController', [
    '$scope', '$uibModalInstance', 'topologyDTO',
    function($scope, $uibModalInstance, topologyDTO) {
      $scope.topologyDTO = topologyDTO;

      $scope.selectedNodeTemplate = undefined;
      $scope.selectedInterface = undefined;
      $scope.selectedOperation = undefined;

      $scope.nodeTemplateItems = [];
      $scope.interfaceItems = [];
      $scope.operationItems = [];

      for (var nodeId in topologyDTO.topology.nodeTemplates) {
        $scope.nodeTemplateItems.push(nodeId);
      }

      $scope.selectNodeTemplate = function(nodeId) {
        $scope.selectedNodeTemplate = nodeId;
        $scope.interfaceItems = [];
        $scope.operationItems = [];
        $scope.selectedInterface = undefined;
        $scope.selectedOperation = undefined;
        // fill $scope.interfaceItems
        var nodeType = $scope.topologyDTO.topology.nodeTemplates[nodeId].type;
        var interfaceName;
        for (interfaceName in $scope.topologyDTO.nodeTypes[nodeType].interfaces) {
          if ($scope.interfaceItems.indexOf(interfaceName) < 0) {
            $scope.interfaceItems.push(interfaceName);
          }
        }
        // some interfaces can be defined at node template level
        for (interfaceName in $scope.topologyDTO.topology.nodeTemplates[nodeId].interfaces) {
          if ($scope.interfaceItems.indexOf(interfaceName) < 0) {
            $scope.interfaceItems.push(interfaceName);
          }
        }
      };

      $scope.selectInterface = function(interfaceName) {
        $scope.selectedInterface = interfaceName;
        $scope.operationItems = [];
        $scope.selectedOperation = undefined;
        // fill $scope.operationItems
        var nodeType = $scope.topologyDTO.topology.nodeTemplates[$scope.selectedNodeTemplate].type;
        var operationName;
        if ($scope.topologyDTO.nodeTypes[nodeType].interfaces[interfaceName]) {
          for (operationName in $scope.topologyDTO.nodeTypes[nodeType].interfaces[interfaceName].operations) {
            if ($scope.operationItems.indexOf(operationName) < 0) {
              $scope.operationItems.push(operationName);
            }
          }
        }
        if ($scope.topologyDTO.topology.nodeTemplates[$scope.selectedNodeTemplate].interfaces && $scope.topologyDTO.topology.nodeTemplates[$scope.selectedNodeTemplate].interfaces[interfaceName]) {
          for (operationName in $scope.topologyDTO.topology.nodeTemplates[$scope.selectedNodeTemplate].interfaces[interfaceName].operations) {
            if ($scope.operationItems.indexOf(operationName)) {
              $scope.operationItems.push(operationName);
            }
          }
        }
      };

      $scope.selectOperation = function(operationName) {
        $scope.selectedOperation = operationName;
      };

      $scope.submit = function() {
        $uibModalInstance.close({node: $scope.selectedNodeTemplate, interface: $scope.selectedInterface, operation: $scope.selectedOperation});
      };

      $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ]);
});
