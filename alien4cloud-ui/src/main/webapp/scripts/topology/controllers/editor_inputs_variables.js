// Editor history browser.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/topology/directives/editor_inputs');
  require('scripts/topology/directives/editor_variables');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('TopologyInputsVariablesCtrl',
    ['$scope', '$alresource', 'topoEditDisplay', function($scope, $alresource, topoEditDisplay) {
      topoEditDisplay($scope, '#topology_inputs_var');
      var firstLoad = true;
      $scope.$watch('topology.archiveContentTree.children[0]', function(newValue, oldValue){
        if(_.defined(newValue) && (firstLoad || newValue !== oldValue)){
          $scope.triggerVarRefresh = {};
          firstLoad=false;
        }
      });
    }
  ]);
});
