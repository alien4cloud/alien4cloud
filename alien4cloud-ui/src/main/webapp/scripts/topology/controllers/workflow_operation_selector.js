define(function (require) {
  'use strict';

  var modules = require('modules');
  require('angular-ui-ace');

  require('angular-tree-control');

  modules.get('a4c-topology-editor').controller('WfOperationSelectorController', [
    '$scope', '$modalInstance', 'topologyDTO',
    function($scope, $modalInstance, topologyDTO) {
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
        // TODO: fill $scope.interfaceItems
        var nodeType = $scope.topologyDTO.topology.nodeTemplates[nodeId].type;
        for (var interfaceName in $scope.topologyDTO.nodeTypes[nodeType].interfaces) {
          $scope.interfaceItems.push(interfaceName);
        }
      };

      $scope.selectInterface = function(interfaceName) {
        $scope.selectedInterface = interfaceName;
        $scope.operationItems = [];
        $scope.selectedOperation = undefined;
        // TODO: fill $scope.operationItems
        var nodeType = $scope.topologyDTO.topology.nodeTemplates[$scope.selectedNodeTemplate].type;
        for (var operationName in $scope.topologyDTO.nodeTypes[nodeType].interfaces[interfaceName].operations) {
          $scope.operationItems.push(operationName);
        }
      };

      $scope.selectOperation = function(operationName) {
        $scope.selectedOperation = operationName;
      };

      $scope.submit = function() {
        $modalInstance.close({node: $scope.selectedNodeTemplate, interface: $scope.selectedInterface, operation: $scope.selectedOperation});
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]);
});
