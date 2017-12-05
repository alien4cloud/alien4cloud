// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var yaml = require('js-yaml');

  require('scripts/topology/services/topology_variables_service');


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
    ['$scope', 'topologyVariableService', '$http', function($scope, topologyVariableService, $http) {

      //return if no inputs defined in topology
      if(_.isEmpty($scope.topology.topology.inputs)){
        return;
      }
      $scope.topology.mappedInputs = {};
      _.forEach(_.keys($scope.topology.topology.inputs), function(inputName){
        $scope.topology.mappedInputs[inputName]=null;
      });

      // console.log(selectedUrl);
      // console.log($scope.topology.archiveContentTree);
      var root = $scope.topology.archiveContentTree.children[0];
      // console.log(topologyVariableService.getInputs(root));
      if(_.defined(topologyVariableService.getInputs(root))){
        $http({method: 'GET',
          transformResponse: function(d) { return d; },
          url: topologyVariableService.getInputsPath($scope.topology.topology.archiveName, $scope.topology.topology.archiveVersion)})
        .then(function(result) {
          // console.log(result.data);
          var loadedInputs =  yaml.safeLoad(result.data);
          _.forEach(_.keys($scope.topology.mappedInputs), function(inputName){
            $scope.topology.mappedInputs[inputName] = yaml.safeDump(loadedInputs[inputName]);
          } );

        });
      }

    }
  ]);
}); // define
