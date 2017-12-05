// Editor history browser.
define(function (require) {
  'use strict';

  var modules = require('modules');

  require('scripts/topology/directives/editor_inputs');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('TopologyInputsVariablesCtrl',
    ['$scope', '$alresource', 'topoEditDisplay', function($scope, $alresource, topoEditDisplay) {
      topoEditDisplay($scope, '#topology_inputs_var');
    }
  ]);
});
