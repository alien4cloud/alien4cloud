// topology templates list is the entry point for browsing and managing global topologies in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-catalog', ['ui.router', 'ui.bootstrap', 'a4c-auth', 'a4c-common']).controller('a4cSearchTopologyCtrl',
    ['$scope', '$translate', 'searchServiceFactory', '$alresource',
    function($scope, $translate, searchServiceFactory, $alresource) {

      $scope.queryManager = {
        labelPrefix: 'TOPOLOGY.',
        additionalBody: {
          type: $scope.componentType
        },
        facetIdConverter: {
          toFilter: function (termId, facetId) {
            return facetId;
          },
          toDisplay: function (termId, facetId) {
            var self = this;
            if (_.isArray(facetId)) {
              //process each value of the array
              return _.transform(facetId, function (result, n) {
                result.push(self.toDisplay(termId, n));
              }, []);
            } else {
              if (_.undefined(facetId)) {
                return $translate.instant('N/A');
              } else {
                return facetId;
              }
            }
          },
          toDisplayFacet: function (termId, filterPrefix) {
            var fullTerm;
            if (termId.startsWith('portability')) {
              fullTerm = termId;
            } else {
              fullTerm = filterPrefix + termId;
            }
            return $translate.instant(fullTerm.toUpperCase());
          }
        }
      };

      // 20 results per page, 10 pages, API does not support pagination (false)
      $scope.searchService = searchServiceFactory('rest/latest/catalog/topologies/search', false, $scope.queryManager, 20, 10, false);

      $scope.$watch('queryManager.searchResult', function (searchResult) {
        // inject selected version for each result
        if(_.defined(searchResult)) {
          _.each(searchResult.data, function (component) {
            component.selectedVersion = component.archiveVersion;
          });
        }
      });

      var versionFetchResource = $alresource('rest/latest/catalog/topologies/:archiveName/versions');
      $scope.fetchElementVersion = function(topology, $event) {
        if(_.defined($event)){
          $event.stopPropagation();
        }
        if(_.defined(topology.olderVersions)) {
          return;
        }
        versionFetchResource.get({archiveName: topology.archiveName},function(result) {
          if(_.defined(result.error)) {
            console.error('Encountered error while fetching element versions', topology.archiveName, result.error);
          } else {
            topology.olderVersions = result.data;
          }
        });
      };

      var topologyResource = $alresource('rest/latest/catalog/topologies/:id');
      $scope.selectVersion = function(topology, newVersion, index, event) {
        if (_.defined(event)) {
          event.stopPropagation();
        }
        if(topology.archiveVersion !== newVersion.version) {
          topologyResource.get({id: newVersion.id},
            function(response) {
            response.data.olderVersions = topology.olderVersions;
            response.data.selectedVersion = newVersion.version;
            $scope.queryManager.searchResult.data[index] = response.data;
          });
        }
      };

      $scope.handleItemSelection = function(topology) {
        $scope.onSelectItem({topology: topology});
      };

    }
  ]); // controller
}); // define
