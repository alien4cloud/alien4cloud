define(function (require) {
  'use strict';

  var angular = require('angular');
  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/tosca/services/tosca_service');
  require('scripts/common/services/search_service_factory');

  modules.get('a4c-catalog', ['a4c-tosca', 'a4c-search']).controller('a4cSearchComponentCtrl', ['$scope', '$alresource', 'toscaService', 'searchServiceFactory', '$state', '$translate',
    function ($scope, $alresource, toscaService, searchServiceFactory, $state, $translate) {
      var alienInternalTags = ['icon'];
      $scope.getIcon = toscaService.getIcon;

      $scope.queryManager = {
        labelPrefix: 'COMPONENTS.',
        additionalBody: {
          type: $scope.componentType
        },
        facetIdConverter: {
          toFilter: function (termId, facetId) {
            if (termId === 'abstract') {
              return facetId !== 'F';
            } else {
              return facetId;
            }
          },
          toDisplay: function (termId, facetId) {
            var self = this;
            if (_.isArray(facetId)) {
              //process each value of the array
              return _.transform(facetId, function (result, n) {
                result.push(self.toDisplay(termId, n));
              }, []);
            } else {
              if (termId === 'abstract') {
                if (facetId === 'F' || facetId === false) { // jshint ignore:line
                  return $translate.instant('FALSE');
                } else {
                  return $translate.instant('TRUE');
                }
              } else if (_.undefined(facetId)) {
                return $translate.instant('N/A');
              } else {
                return facetId;
              }
            }
          },
          toDisplayFacet: function (termId, filterPrefix) {
            var fullTerm;

            if (termId.startsWith("metaProperties.")) {
                return termId.substring(15);
            }

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
      $scope.searchService = searchServiceFactory('rest/latest/components/search', false, $scope.queryManager, 20, 10, false);

      $scope.$watch('queryManager.searchResult', function (searchResult) {
        // inject selected version for each result
        if(_.defined(searchResult)) {
          _.each(searchResult.data, function (component) {
            component.selectedVersion = component.archiveVersion;
          });
        }
      });

      var badges = _.clone($scope.badges) || [];
      // abstract badge is always displayed
      badges.push({
        name: 'abstract',
        tooltip: 'COMPONENTS.COMPONENT.ABSTRACT_COMPONENT',
        imgSrc: 'images/abstract_ico.png',
        canDislay: function (component) {
          return component.abstract;
        },
        priority: 0
      });
      //sort by priority
      $scope.badges = _.sortBy(badges, 'priority');

      /**
       * search handlers
       */

      /*update a search*/
      function updateSearch(filters, force) {
        /*
         Search api expect a json object matching the following pattern:
         {
         'query': 'mysearched',
         'from' : int,
         'to' : int,
         'filters': {'termId1' : ['facetId1'],'termId2' : ['facetId2','facetedId3'] }
         }
         */

        // Convert filter [] filters -> Object
        var objectFilters = {};
        filters.forEach(function (filter) {
          filter = filter || {};
          if (!(filter.term in objectFilters)) {
            // First time the key is present set to the value in filter
            objectFilters[filter.term] = filter.facet;
          } else {
            // Merge otherwise
            objectFilters[filter.term].push.apply(objectFilters[filter.term], filter.facet);
          }
        });

        var searchRequestObject = {
          'type': $scope.queryComponentType
        };
        $scope.filters = objectFilters;
        $scope.searchService.search(null, searchRequestObject, !force);
      }

      /* trigger a new search, when params are changed */
      $scope.doSearch = function (force) {
        var allFacetFilters = [];
        allFacetFilters.push.apply(allFacetFilters, $scope.facetFilters);
        if (angular.isDefined($scope.hiddenFilters)) {
          allFacetFilters.push.apply(allFacetFilters, $scope.hiddenFilters);
        }
        updateSearch(allFacetFilters, force);
      };

      //on search completed
      $scope.queryManager.onSearchCompleted = function (searchResponse) {
        if(_.defined(searchResponse.data) ) {
          // inject static facets
          _.forEach($scope.staticFacets, function (facet, facetKey) {
            searchResponse.data.facets[facetKey] = facet;
          });
          $scope.queryManager.searchResult = searchResponse.data;
        }else {
          $scope.queryManager.searchResult = undefined;
        }
      };

      var versionFetchResource = $alresource('rest/latest/components/element/:elementId/versions');
      $scope.fetchElementVersion = function (component, $event) {
        if(_.defined($event)){
          $event.stopPropagation();
        }
        if (_.defined(component.olderVersions)) {
          return;
        }
        versionFetchResource.get({
          elementId: component.elementId,
          toscaType: $scope.queryComponentType
        }, function (result) {
          if (_.defined(result.error)) {
            console.error('Encountered error while fetching element versions', component.elementId, result.error);
          } else {
            component.olderVersions = result.data;
          }
        });
      };

      // Getting full search result from /data folder


      /*Remove a facet filter*/
      $scope.removeFilter = function (filterToRemove) {
        // Remove the selected filter
        _.remove($scope.facetFilters, filterToRemove);
        // Search update with new filters list
        $scope.doSearch();
      };

      /*Reset all filters*/
      $scope.reset = function () {
        // Reset all filters
        $scope.facetFilters.splice(0, $scope.facetFilters.length);
        $scope.doSearch();
      };

      /** check if this component is default for a capability */
      $scope.isADefaultCapability = function (component, capability) {
        if (component.defaultCapabilities) {
          return (component.defaultCapabilities.indexOf(capability) >= 0);
        }
      };

      /* Restrict tags visibility */
      $scope.isInternalTag = function (tag) {
        return alienInternalTags.indexOf(tag) >= 0;
      };

      $scope.handleItemSelection = function (item) {
        if (!$scope.dragAndDropEnabled) {
          $scope.onSelectItem({
            item: item
          });
        }
      };



      var componentResource = $alresource('rest/latest/components/:id');
      $scope.selectVersion = function (component, newVersion, index, event) {
        if (event) {
          event.stopPropagation();
        }
        if (component.archiveVersion !== newVersion.version) {
          // Retrieve the other version
          componentResource.get({
            id: newVersion.id
          }, function (successResult) {
            // override the component with the retrieved version
            var selectedVersionComponent = successResult.data;
            selectedVersionComponent.olderVersions = component.olderVersions;
            selectedVersionComponent.selectedVersion = newVersion.version;
            $scope.queryManager.searchResult.data[index] = selectedVersionComponent;
          });
        }
      };

      $scope.handleBadgeClick = function (badge, component, event) {
        if (_.isFunction(badge.onClick)) {
          if (event) {
            event.stopPropagation();
          }
          badge.onClick(component, $state);
        }
      };

    }]); // controller
}); // define
