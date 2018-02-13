define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  var registerEditorSubstates = require('scripts/topology/editor_register_service');

  states.state('editor_catalog_topology', {
    url: '/catalog/topologies/:archiveId',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploynext_topology_editor.html',
    controller: 'CatalogTopoEditorCtrl'
  });

  // Define editor states from root (to use full-screen and avoid dom and scopes pollution)
  states.state('editor_catalog_topology.editor', {
    url: '',
    templateUrl: 'views/topology/topology_editor_layout.html',
    controller: 'TopologyEditorCtrl'
  });
  registerEditorSubstates('editor_catalog_topology.editor', {inputs: {show: false}});
  states.forward('editor_catalog_topology', 'editor_catalog_topology.editor');

  modules.get('a4c-catalog').controller('CatalogTopoEditorCtrl',
    ['$scope', '$state', '$stateParams', 'userContextServices', 'applicationServices', 'applicationEnvironmentServices', 'breadcrumbsService','$translate',
    function ($scope, $state, $stateParams, userContextServices, applicationServices, applicationEnvironmentServices, breadcrumbsService, $translate) {

      breadcrumbsService.registerMapping('editor_catalog_topology.', 'catalog.topologies.');
      breadcrumbsService.putConfig({
        state: 'catalog.topologies',
        text: function () {
          return $translate.instant('NAVCATALOG.TOPOLOGIES');
        }
      });
      breadcrumbsService.putConfig({
        state: 'catalog.topologies.editor',
        text: function () {
          return $stateParams.archiveId;
        }
      });
    }
  ]);
});
