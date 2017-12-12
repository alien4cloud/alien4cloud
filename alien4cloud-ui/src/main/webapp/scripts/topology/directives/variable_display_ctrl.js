// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var yaml = require('js-yaml');

  require('scripts/topology/services/topology_variables_service');
  require('scripts/topology/services/topology_browser_service');
  require('scripts/common/filters/a4c_linky');


  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('variableDisplayCtrl',
    ['$scope', '$uibModal', 'varName', '$uibModalInstance', 'topologyVariableService', '$alresource',
      function($scope,  $uibModal, varName, $uibModalInstance, topologyVariableService, $alresource) {

        var appVariablesService = $alresource('/rest/applications/:applicationId/variables/:varName');
        var envVariableService = $alresource('/rest/latest/applications/:applicationId/topologyVersion/:topologyVersion/environments/variables/:varName');
        var envTypeVariableService = $alresource('/rest/latest/applications/:applicationId/topologyVersion/:topologyVersion/environmentTypes/variables/:varName');
        var aceEditor;

        function fetchInApplicationScope(){
          appVariablesService.get({
            applicationId: $scope.topology.topology.archiveName,
            varName: varName
          }, function(result){
            $scope.appScopeDTO = result.data;
          });
        }

        function fetchInEnvironementScope() {
          envVariableService.get({
            applicationId: $scope.topology.topology.archiveName,
            topologyVersion: $scope.topology.topology.archiveVersion,
            varName: varName,
          }, function(result){
            $scope.envScopeDTO = result.data;
            // console.log('Loaded for envs: ', result.data);
          });
        }

        function fetchInEnvironementTypeScope() {
          envTypeVariableService.get({
            applicationId: $scope.topology.topology.archiveName,
            topologyVersion: $scope.topology.topology.archiveVersion,
            varName: varName,
          }, function(result){
            $scope.envTypeScopeDTO = result.data;
            // console.log('Loaded for envs types: ', result.data);
          });
        }

        function refresh(varName) {
          $scope.varName = varName;
          $scope.envScopeDTO=undefined;
          $scope.envTypeScopeDTO = undefined;
          $scope.appScopeDTO=undefined;
          $scope.selectedScope=undefined;

          fetchInApplicationScope();
          fetchInEnvironementScope();
          fetchInEnvironementTypeScope();
        }

        function showVarExpression(selectedScope, expression) {
          $scope.selectedScope = selectedScope;
          aceEditor.getSession().setValue(expression || '');
        }

        $scope.showAppVarExpression = function(expression){
          showVarExpression({
            scope:'APP',
            name: $scope.topology.topology.archiveName,
            id:$scope.topology.topology.archiveName
          }, expression);
        };

        $scope.showEnvVarExpression = function(varDTO){
          showVarExpression({
            scope:'ENV',
            name: varDTO.scopeName,
            id:varDTO.scopeId
          }, varDTO.variable.expression);
        };

        $scope.showEnvTypeVarExpression = function(varDTO){
          showVarExpression({
            scope:'ENV_TYPE',
            name: varDTO.scopeName,
            id:varDTO.scopeId
          }, varDTO.variable.expression);
        };
        $scope.ok = function() {
          var vars = {};
          vars[varName]='toto';
          $uibModalInstance.close(vars);
        };

        $scope.cancel = function() {
          $uibModalInstance.dismiss('canceled');
        };

        refresh(varName);

        $scope.aceLoaded = function(_editor) {
          aceEditor = _editor;
          _editor.commands.addCommand({
            name: 'save',
            bindKey: {win: 'Ctrl-S', mac: 'Command-S'},
            exec: function() {
              $scope.saveEdited();
            }
          });
        };

        $scope.collapsed = {env: true, envType: false};

        $scope.toggle = function(name){
          var collapsed = $scope.collapsed[name];
          _.each($scope.collapsed, function(value, key){
            $scope.collapsed[key]=true;
          });
          $scope.collapsed[name] = !collapsed;
        };

        function execute(operation){
          $scope.execute(operation);
        }

        $scope.saveEdited = function(){
          var operation = {name: $scope.varName};
          switch ($scope.selectedScope.scope) {
            case 'ENV':
              operation.type = 'org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation';
              operation.environmentId=$scope.selectedScope.id;
              operation.expression=aceEditor.getSession().getDocument().getValue();
              execute(operation);
              break;
            case 'ENV_TYPE':
              operation.type = 'org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentTypeVariableOperation';
              operation.environmentType=$scope.selectedScope.id;
              operation.expression=aceEditor.getSession().getDocument().getValue();
              execute(operation);
              break;
            default:
              console.error('Not yet supported: ', $scope.selectedScope.scope);
          }
        };

    }
  ]);
}); // define
