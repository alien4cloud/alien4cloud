// Editor history browser.
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-components', ['a4c-common', 'ui.ace', 'treeControl']).controller('TopologyHistoryCtrl',
    ['$scope', 'topoEditDisplay', function($scope, topoEditDisplay) {
      $scope.displays = {
        history: { active: true, size: 400, selector: '#history-box', only: [] },
      };
      topoEditDisplay($scope, '#history-explorer');

      $scope.selectOperation = function(operation) {
        $scope.selectedOperation = operation;
      };
  }]);
});
