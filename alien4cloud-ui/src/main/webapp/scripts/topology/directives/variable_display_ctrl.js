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
    ['$scope', '$uibModal', 'varName', '$uibModalInstance', 'topologyVariableService', '$alresource', '$filter',
      function($scope,  $uibModal, varName, $uibModalInstance, topologyVariableService, $alresource, $filter) {

        var appVariablesService = $alresource('/rest/applications/:applicationId/variables/:varName');
        var envVariableService = $alresource('/rest/latest/applications/:applicationId/topologyVersion/:topologyVersion/environments/variables/:varName');
        var envTypeVariableService = $alresource('/rest/latest/applications/:applicationId/topologyVersion/:topologyVersion/environmentTypes/variables/:varName');
        var aceEditor;

        $scope.editMode = false;

        function fetchInApplicationScope(){
          appVariablesService.get({
            applicationId: $scope.topology.topology.archiveName,
            varName: $scope.varName
          }, function(result){
            $scope.appScopeDTO = result.data;
            // console.log('Loaded for app: ', $scope.appScopeDTO);
          });
        }

        function fetchInEnvironementScope() {
          envVariableService.get({
            applicationId: $scope.topology.topology.archiveName,
            topologyVersion: $scope.topology.topology.archiveVersion,
            varName: $scope.varName,
          }, function(result){
            $scope.envScopeDTO = result.data;
            // console.log('Loaded for envs: ', $scope.envScopeDTO);
          });
        }

        function fetchInEnvironementTypeScope() {
          envTypeVariableService.get({
            applicationId: $scope.topology.topology.archiveName,
            topologyVersion: $scope.topology.topology.archiveVersion,
            varName: $scope.varName,
          }, function(result){
            $scope.envTypeScopeDTO = result.data;
            // console.log('Loaded for envs types: ', $scope.envTypeScopeDTO);
          });
        }

        function refresh(selectedVarName) {
          // console.log('Refreshed called for: ', selectedVarName);
          $scope.varName = selectedVarName;
          $scope.envScopeDTO=undefined;
          $scope.envTypeScopeDTO = undefined;
          $scope.appScopeDTO=undefined;
          $scope.selectedScope=undefined;

          fetchInApplicationScope();
          fetchInEnvironementScope();
          fetchInEnvironementTypeScope();
        }

        function setAceEditorContent(){
          if(_.defined($scope.selectedScope)){
            $scope.selectedScope.editorContent = $scope.selectedScope.expression;
          }
        }

        function showVarExpression(selectedScope, expression) {
          $scope.selectedScope = selectedScope;
          $scope.selectedScope.expression = expression;
          setAceEditorContent();
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

        $scope.cancel = function() {
          $uibModalInstance.dismiss('closed');
        };

        $scope.aceLoaded = function(_editor) {
          aceEditor = _editor;
          _editor.commands.addCommand({
            name: 'save',
            bindKey: {win: 'Ctrl-S', mac: 'Command-S'},
            exec: function() {
              $scope.saveEdited();
            }
          });
          setAceEditorContent();
        };

        $scope.collapsed = {env: true, envType: false};

        $scope.toggle = function(name){
          var collapsed = $scope.collapsed[name];
          _.each($scope.collapsed, function(value, key){
            $scope.collapsed[key]=true;
          });
          $scope.collapsed[name] = !collapsed;
        };

        $scope.saveEdited = function(){
          var operation = {name: $scope.varName};
          switch ($scope.selectedScope.scope) {
            case 'ENV':
              operation.type = 'org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation';
              operation.environmentId=$scope.selectedScope.id;
              operation.expression=aceEditor.getSession().getDocument().getValue();
              $scope.execute(operation);
              $scope.selectedScope.expression=operation.expression;
              break;
            case 'ENV_TYPE':
              operation.type = 'org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentTypeVariableOperation';
              operation.environmentType=$scope.selectedScope.id;
              operation.expression=aceEditor.getSession().getDocument().getValue();
              $scope.execute(operation);
              $scope.selectedScope.expression=operation.expression;
              break;
            default:
              console.error('Not yet supported: ', $scope.selectedScope.scope);
          }
        };

        $scope.toggleEditMode = function(){
          $scope.editMode = !$scope.editMode;
        };

        $scope.refreshSelectedVar= refresh;

        $scope.dump = function() {
          return $filter('a4cLinky')($scope.selectedScope.expression, 'refreshSelectedVar');
        };

        //first load
        refresh(varName);

    }
  ]);
}); // define
