// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var yaml = require('js-yaml');

  require('scripts/topology/directives/variable_display_ctrl');

  modules.get('a4c-topology-editor').directive('editorVariables',
    [
    function() {
      return {
        restrict: 'E',
        templateUrl: 'views/topology/inputs/editor_variables.html',
        controller: 'editorVariablesCtrl'
      };
    }
  ]); // directive


  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace']).controller('editorVariablesCtrl',
    ['$scope', '$uibModal', '$alresource', function($scope, $uibModal, $alresource) {

      var envVariableService = $alresource('/rest/latest/applications/:applicationId/topologyVersion/:topologyVersion/variables');

      function refresh(){
        envVariableService.get({
          applicationId: $scope.topology.topology.archiveName,
          topologyVersion: $scope.topology.topology.archiveVersion,
        }, function(result){
          $scope.variables = result.data;
        });
      }

      $scope.openVarModal = function(varName){
        $uibModal.open({
          templateUrl: 'views/topology/variables/variable_value_display.html',
          controller: 'variableDisplayCtrl',
          scope: $scope,
          size: 'lg',
          resolve: {
            varName: function() {
              return varName;
            }
          }
        });
      };

      $scope.addVariable = function(variable){
          $scope.openVarModal(variable.name);
          if(_.indexOf($scope.variables, variable.name) < 0){
            $scope.variables.push(variable.name);
          }
          variable.name='';
      };


      $scope.$watch('triggerVarRefresh', function(newValue){
        if(_.defined(newValue)){
          refresh();
        }
      });
    }
  ]);
}); // define
