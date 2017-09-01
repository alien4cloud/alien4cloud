// topology templates list is the entry point for browsing and managing global topologies in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/_ref/catalog/directives/topologies_catalog');

  states.state('catalog.topologies.list', {
    url: '/list',
    templateUrl: 'views/_ref/catalog/topologies/topologies_list.html',
    controller: 'A4CTopologiesListCtrl'
  });

  modules.get('a4c-topology-templates', ['ui.router', 'ui.bootstrap', 'a4c-auth', 'a4c-common']).controller('A4CTopologiesListCtrl',
    ['$scope', '$state',
    function($scope, $state) {
      $scope.onSelect = function(topology) {
        $state.go('topologycatalog.csar', { archiveName: topology.archiveName, archiveVersion: topology.archiveVersion });
      };
    }
  ]); // controller
}); // define
