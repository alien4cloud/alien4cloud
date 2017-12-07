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
        var envVariableService = $alresource('/rest/latest/applications/:applicationId/environments/variables/:varName');
        var aceEditor;

        function fetchInApplicationScope(){
          appVariablesService.get({
            applicationId: $scope.topology.topology.archiveName,
            varName: varName
          }, function(result){
            $scope.appScope = result.data;
          });
        }

        function fetchInEnvironementScope() {
          envVariableService.get({
            applicationId: $scope.topology.topology.archiveName,
            varName: varName,
          }, function(result){
            $scope.envVarDefs = result.data;
            console.log('Loaded for envs: ', result.data);
          });
        }

        function refresh(varName) {
          $scope.varName = varName;
          console.log($scope.varName);
          $scope.envScope=undefined;
          $scope.envTypeScope = undefined;
          $scope.appScope=undefined;

          fetchInApplicationScope();
          fetchInEnvironementScope();
        }

        function showVarExpression(selectedScope, expression) {
          $scope.selectedScope = selectedScope;
          aceEditor.getSession().setValue(expression || '');
        }

        $scope.showAppVarExpression = function(expression){
          showVarExpression({scope:'APP', id:$scope.topology.topology.archiveName}, expression);
        };

        $scope.showEnvVarExpression = function(envId, expression){
          showVarExpression({scope:'ENV', id:envId}, expression);
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
              console.log('save trigered ', aceEditor.getSession().getDocument().getValue());
              console.log('selected scope: ', $scope.selectedScope);
            }
          });
        };


    }
  ]);
}); // define
