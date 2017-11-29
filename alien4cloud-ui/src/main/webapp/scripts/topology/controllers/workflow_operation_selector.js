define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('angular-ui-ace');

  require('angular-tree-control');

  modules.get('a4c-topology-editor').controller('WfOperationSelectorController', [
    '$scope', '$uibModalInstance', 'topologyDTO',
    function ($scope, $uibModalInstance, topologyDTO) {
      $scope.topologyDTO = topologyDTO;

      $scope.selectedNodeTemplate = undefined;
      $scope.selectedInterface = undefined;
      $scope.selectedOperation = undefined;

      $scope.nodeTemplateItems = [];
      $scope.relationshipTemplateItems = [];
      $scope.interfaceItems = [];
      $scope.operationItems = [];

      for (var nodeId in topologyDTO.topology.nodeTemplates) {
        $scope.nodeTemplateItems.push(nodeId);
      }

      var fillInterfaceItems = function (interfaces) {
        for (var nodeInterfaceName in interfaces) {
          if ($scope.interfaceItems.indexOf(nodeInterfaceName) < 0) {
            $scope.interfaceItems.push(nodeInterfaceName);
          }
        }
      };

      var fillOperationItems = function (interfaces, interfaceName) {
        if (interfaces && interfaces[interfaceName]) {
          for (var operationName in interfaces[interfaceName].operations) {
            if ($scope.operationItems.indexOf(operationName)) {
              $scope.operationItems.push(operationName);
            }
          }
        }
      };

      $scope.selectNodeTemplate = function () {
        $scope.interfaceItems = [];
        $scope.operationItems = [];
        $scope.relationshipTemplateItems = [];
        delete $scope.selectedInterface;
        delete $scope.selectedOperation;
        delete $scope.selectedRelationship;
        // fill $scope.interfaceItems
        var nodeTemplate = $scope.topologyDTO.topology.nodeTemplates[$scope.selectedNodeTemplate];
        var nodeType = $scope.topologyDTO.nodeTypes[nodeTemplate.type];
        fillInterfaceItems(nodeType.interfaces);
        // some interfaces can be defined at node template level
        fillInterfaceItems(nodeTemplate.interfaces);
        if (nodeTemplate.relationshipsMap) {
          for (var relationship in nodeTemplate.relationshipsMap) {
            $scope.relationshipTemplateItems.push(relationship);
          }
        }
      };

      $scope.selectRelationship = function () {
        $scope.interfaceItems = [];
        $scope.operationItems = [];
        delete $scope.selectedInterface;
        delete $scope.selectedOperation;
        var nodeTemplate = $scope.topologyDTO.topology.nodeTemplates[$scope.selectedNodeTemplate];
        var relationshipTemplate = nodeTemplate.relationshipsMap[$scope.selectedRelationship].value;
        var relationshipType = $scope.topologyDTO.relationshipTypes[relationshipTemplate.type];
        fillInterfaceItems(relationshipTemplate.interfaces);
        // some interfaces can be defined at node template level
        fillInterfaceItems(relationshipType.interfaces);
      };

      $scope.selectInterface = function () {
        $scope.operationItems = [];
        delete $scope.selectedOperation;
        var nodeTemplate = $scope.topologyDTO.topology.nodeTemplates[$scope.selectedNodeTemplate];
        var nodeType = $scope.topologyDTO.nodeTypes[nodeTemplate.type];

        if (_.isEmpty($scope.selectedRelationship)) {
          fillOperationItems(nodeType.interfaces, $scope.selectedInterface);
          fillOperationItems(nodeTemplate.interfaces, $scope.selectedInterface);
        } else {
          var relationshipTemplate = nodeTemplate.relationshipsMap[$scope.selectedRelationship].value;
          var relationshipType = $scope.topologyDTO.relationshipTypes[relationshipTemplate.type];
          fillOperationItems(relationshipType.interfaces, $scope.selectedInterface);
          fillOperationItems(relationshipTemplate.interfaces, $scope.selectedInterface);
        }
      };

      $scope.submit = function () {
        $uibModalInstance.close({
          node: $scope.selectedNodeTemplate,
          interface: $scope.selectedInterface,
          targetRelationship: $scope.selectedRelationship,
          operation: $scope.selectedOperation
        });
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }
  ]);
});
