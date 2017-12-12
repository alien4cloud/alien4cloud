// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var yaml = require('js-yaml');

  require('scripts/topology/services/topology_variables_service');
  require('scripts/topology/services/topology_browser_service');
  require('scripts/topology/directives/variable_display_ctrl');
  require('scripts/common/filters/a4c_linky');

  modules.get('a4c-topology-editor').directive('editorInputs',
    [
    function() {
      return {
        restrict: 'E',
        templateUrl: 'views/topology/inputs/editor_inputs.html',
        controller: 'editorInputsCtrl'
      };
    }
  ]); // directive


  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('editorInputsCtrl',
    ['$scope', 'topologyVariableService', '$http', 'topoBrowserService', '$filter', '$uibModal', function($scope, topologyVariableService, $http, topoBrowserService, $filter, $uibModal) {

      $scope.dump = function(value) {
        return $filter('a4cLinky')(_.trim(yaml.safeDump(value, {indent: 4}), '"\n'), 'openVarModal');
      };

      function refresh(expanded){
        var inputsFileNode = topologyVariableService.getInputs(expanded);
        // var inputsFileNode = topologyVariableService.getInputs($scope.topology.archiveContentTree.children[0]);

        if(_.defined(inputsFileNode)){
          topoBrowserService.getContent($scope.topology.topology, inputsFileNode, function(result){
            $scope.loadedInputs= yaml.safeLoad(result.data);
          });
        }
      }

      function updateInputFile(content) {
        $scope.execute({
          type: 'org.alien4cloud.tosca.editor.operations.UpdateFileContentOperation',
          path: 'inputs/inputs.yml',
          content: content
        });
      }

      $scope.clearInput = function(inputName) {
        if(_.has($scope.loadedInputs, inputName)){
          delete $scope.loadedInputs[inputName];
          var content = yaml.safeDump($scope.loadedInputs);
          updateInputFile(content);
        }
      };

      $scope.openVarModal = function(varName){
        var modalInstance = $uibModal.open({
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
        modalInstance.result.then(function(vars) {
          console.log(vars);
          // var gitPullResource= $alresource('rest/latest/editor/:topologyId/git/pull');
          // gitPullResource.update({topologyId: $scope.topologyId, remoteBranch: gitPushPullForm.remoteBranch}, angular.toJson(gitPushPullForm.credentials), function(response) {
          //   if(_.undefined(response.error)){
          //     $scope.refreshTopology(response.data);
          //     toaster.pop('success', $translate.instant('EDITOR.GIT.OPERATIONS.PULL.TITLE'), $translate.instant('EDITOR.GIT.OPERATIONS.PULL.SUCCESS_MSGE'), 4000, 'trustedHtml', null);
          //   } else {
          //     $scope.showParsingErrors(response);
          //   }
          // });
        });
      };


      var firstLoad = true;
      $scope.$watch('topology.archiveContentTree.children[0]', function(newValue, oldValue){
        if(_.defined(newValue) && (firstLoad || newValue !== oldValue)){
          refresh(newValue);
          firstLoad=false;
        }
      });
    }
  ]);
}); // define
