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
        $scope.onSearchConfig({ searchConfig: searchConfig });
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

      var topologyResource = $alresource('rest/latest/catalog/topologies/:id');
      $scope.selectVersion = function(topology, version, index, event) {
        if (_.defined(event)) {
          event.stopPropagation();
        }
        if(topology.archiveVersion !== version.version) {
          topologyResource.get({id: version.id},
            function(response) {
            response.data.allVersions = topology.allVersions;
            $scope.searchConfig.result.data[index] = response.data;
          });
        }
      };

      $scope.clone= function(topology, event) {
        if (_.defined(event)) {
          event.stopPropagation();
        }
        $scope.onSelectForClone({topology: topology});
      };

      ////////////////////////////////////
      ///  Delete topology template modal
      ///
      var DeleteTopologyTemplateModalCtrl = ['$scope', '$modalInstance', '$state', 'csarId',
        function($scope, $modalInstance, $state, csarId) {
          $scope.csarId = csarId;
          $scope.goToArchive = function () {
            $state.go('components.csars.csardetail', { csarId: csarId });
            $scope.close();
          };

          $scope.close = function () {
            $modalInstance.dismiss();
          };
      }];

      $scope.openDeleteTopologyTemplateModal = function(csar, event) {
        if (_.defined(event)) {
          event.stopPropagation();
        }
        $modal.open({
          templateUrl: 'views/topologytemplates/toplogy_template_remove_modal.html',
          controller: DeleteTopologyTemplateModalCtrl,
          resolve: {
            csarId: function() {
              return csar;
            }
          }
        });
      };

    }
  ]); // controller
}); // define
