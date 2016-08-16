define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-editor', ['ui.bootstrap']).controller('TopologyEditorArtifactModalCtrl', ['$scope', '$modalInstance', 'explorerService','archiveContentTree',
    function($scope, $modalInstance, explorerService, archiveContentTree) {
      $scope.artifact = {};

      $scope.opts = explorerService.getOps(false);
      $scope.treedata = {
        children: [],
        name: 'loading...'
      };

      $scope.onSelect = function(node) {
        var dirName = node.fullPath.substring(node.fullPath.split('/', 2).join('/').length+1);
        $scope.artifact.repository = undefined;
        $scope.artifact.reference = dirName;
      };

      var root = archiveContentTree.children[0];
      $scope.treedata.children = root.children;

      $scope.save = function(valid) {
        if (valid) {
          $modalInstance.close($scope.artifact);
        }
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ]);
});
