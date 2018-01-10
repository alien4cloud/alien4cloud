// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('angular-ui-ace');
  require('scripts/common/directives/ace_save_button');
  require('scripts/topology/services/topology_variables_service');
  require('scripts/topology/services/topology_browser_service');
  require('scripts/common/filters/a4c_linky');


  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace']).controller('variableDisplayCtrl',
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
            $scope.selectedScope.editorContent = {
              old: $scope.selectedScope.variable.expression,
              new: $scope.selectedScope.variable.expression
            };
          }
        }

        function showVarExpression(scopeType, selectedScope) {
          $scope.selectedScope = selectedScope;
          $scope.selectedScope.scope = scopeType;
          setAceEditorContent();
        }

        $scope.showAppVarExpression = function(variable){
          showVarExpression('APP', {
            scopeName: $scope.topology.topology.archiveName,
            scopeId:$scope.topology.topology.archiveName,
            readOnly:true,
            variable: variable
          });
        };

        $scope.showEnvVarExpression = function(varDTO){
          showVarExpression('ENV', varDTO);
        };

        $scope.showEnvTypeVarExpression = function(varDTO){
          showVarExpression('ENV_TYPE', varDTO);
        };

        $scope.close = function() {
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
              operation.environmentId=$scope.selectedScope.scopeId;
              operation.expression=aceEditor.getSession().getDocument().getValue();
              $scope.execute(operation);
              $scope.selectedScope.variable.expression=operation.expression;
              break;
            case 'ENV_TYPE':
              operation.type = 'org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentTypeVariableOperation';
              operation.environmentType=$scope.selectedScope.scopeId;
              operation.expression=aceEditor.getSession().getDocument().getValue();
              $scope.execute(operation);
              $scope.selectedScope.variable.expression=operation.expression;
              break;
            default:
              console.error('Not yet supported: ', $scope.selectedScope.scope);
          }
          setAceEditorContent();
        };

        $scope.toggleEditMode = function(){
          $scope.editMode = !$scope.editMode;
        };

        $scope.refreshSelectedVar= refresh;

        $scope.dump = function() {
          return $filter('a4cLinky')($scope.selectedScope.variable.expression, 'refreshSelectedVar');
        };

        $scope.disableSave = function(){
          return !$scope.editMode || !$scope.selectedScope || _.get($scope.selectedScope, 'readOnly', false);
        };

        //first load
        refresh(varName);

    }
  ]);
}); // define
