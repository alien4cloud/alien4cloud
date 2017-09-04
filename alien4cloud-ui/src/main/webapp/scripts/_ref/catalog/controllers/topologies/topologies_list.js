// topology templates list is the entry point for browsing and managing global topologies in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/_ref/catalog/directives/topologies_catalog');
  require('scripts/_ref/catalog/controllers/topologies/topologies_csar_editor');

  states.state('catalog.topologies.list', {
    url: '/list',
    templateUrl: 'views/_ref/catalog/topologies/topologies_list.html',
    controller: 'A4CTopologiesListCtrl'
  });

  modules.get('a4c-catalog', ['ui.router', 'ui.bootstrap', 'a4c-auth', 'a4c-common']).controller('A4CTopologiesListCtrl',
    ['$scope', '$state', 'breadcrumbsService', '$translate',
    function($scope, $state, breadcrumbsService, $translate) {

      breadcrumbsService.putConfig({
        state: 'catalog.topologies',
        text: function () {
          return $translate.instant('NAVCATALOG.TOPOLOGIES');
        }
      });

      $scope.onSelect = function(topology) {
        $state.go('editor_catalog_topology.editor', { archiveId: topology.id });
      };
    }
  ]); // controller
}); // define
