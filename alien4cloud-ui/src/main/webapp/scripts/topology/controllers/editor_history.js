// Editor history browser.
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('TopologyHistoryCtrl',
    ['$scope', '$alresource', 'topoEditDisplay', function($scope, $alresource, topoEditDisplay) {
      $scope.displays = {
        history: { active: true, size: 400, selector: '#history-box', only: [] }
      };
      topoEditDisplay($scope, '#history-explorer');

      $scope.selectOperation = function(operation) {
        $scope.gitCommits = undefined;
        $scope.selectedOperation = operation;
      };

      var editorGitResource = $alresource('rest/latest/editor/:topologyId/history');
      $scope.gitHistory = function() {
        $scope.selectedOperation = undefined;
        editorGitResource.get({
          topologyId: $scope.topologyId,
          from: 0,
          count: 20
        }, null, function(result){
          $scope.gitCommits = result.data;
        });
      };
    }
  ]);
});
