// topology templates list is the entry point for browsing and managing global topologies in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-templates', ['ui.router', 'ui.bootstrap', 'a4c-auth', 'a4c-common']).controller('TopologyTemplateSearchCtrl',
    ['$scope', '$modal', '$resource', '$state', 'authService', '$alresource',
    function($scope, $modal, $resource, $state, authService, $alresource) {
      $scope.onSearch = function (searchConfig) {
        $scope.searchConfig = searchConfig;
      };
      $scope.openCsar = function(csarId, event) {
        if (_.defined(event)) {
          event.stopPropagation();
        }
        $state.go('components.csars.csardetail', { csarId: csarId });
      };
      var fetchVersionsResource = $alresource('rest/latest/catalog/topologies/:archiveName/versions');
      $scope.fetchVersions = function(topology) {
        if(_.defined(topology.allVersions)) {
          return;
        }
        fetchVersionsResource.get({archiveName: topology.archiveName},function(result) {
          if(_.defined(result.error)) {
            console.error('Encountered error while fetching element versions', topology.archiveName, result.error);
          } else {
            topology.allVersions = result.data;
          }
        });
      };
    }
  ]); // controller
}); // define
