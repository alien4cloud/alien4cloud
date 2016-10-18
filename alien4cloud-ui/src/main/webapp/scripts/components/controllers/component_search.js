define(function (require) {
  'use strict';

  var angular = require('angular');
  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/tosca/services/tosca_service');
  require('scripts/common/services/search_service_factory');

  modules.get('a4c-components', ['a4c-tosca', 'a4c-search']).controller('alienSearchComponentCtrl', ['$scope', '$filter', '$alresource', '$resource', 'toscaService', 'searchServiceFactory', '$state', '$translate',
    function($scope, $filter, $alresource, $resource, toscaService, searchServiceFactory, $state, $translate) {
      var alienInternalTags = ['icon'];
      $scope.searchService = searchServiceFactory('rest/latest/components/search', false, $scope, 20, 10, false);
      $scope.searchService.filtered(true);

      var badges = $scope.badges || [];
      // abstract badge is always displayed
      badges.push({
        name: 'abstract',
        tooltip: 'COMPONENTS.COMPONENT.ABSTRACT_COMPONENT',
        imgSrc: 'images/abstract_ico.png',
        canDislay: function(component){
          return component.abstract;
        },
        priority: 0
      });
      //sort by priority
      $scope.badges = _.sortBy(badges, 'priority');

      $scope.translateKey = function(term) {
        var upterm = term.toUpperCase();
        if(upterm.indexOf('PORTABILITY') > -1) {
          return $translate.instant(upterm);
        }

        return $translate.instant('COMPONENTS.'+upterm);
      };

      /** Used to display the correct text in UI */
      $scope.facetIdConverter = {
        toFilter: function(termId, facetId) {
          return facetId;
        },
        toDisplay: function(termId, facetId) {
          if (termId === 'abstract') {
            if (facetId == 'F') { // jshint ignore:line
              return $filter('translate')('FALSE');
            } else {
              return $filter('translate')('TRUE');
            }
          } else if (_.undefined(facetId)) {
            return $filter('translate')('N/A');
          } else {
            return facetId;
          }
        },
        toDisplayFacet: function(termId, filterPrefix) {
          if (termId.startsWith('portability')) {
            // should not prefix the traduction key from plugin
            return $filter('translate')($filter('uppercase')(termId));
          } else {
            return $filter('translate')($filter('uppercase')(filterPrefix + termId));
          }
        }
      };

      $scope.setComponent = function(component) {
        $scope.detailComponent = component;
      };

      /**
       * search handlers
       */
      //bind the scope search vars to the searchContext service
      $scope.query = '';
      $scope.facetFilters = [];

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
        filters.forEach(function(filter) {

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

      $scope.forcedDoSearch= function() {
        $scope.doSearch(true);
      };

      /*trigger a new search, when params are changed*/
      $scope.doSearch = function(force) {
        var allFacetFilters = [];
        allFacetFilters.push.apply(allFacetFilters, $scope.facetFilters);
        if (angular.isDefined($scope.hiddenFilters)) {
          allFacetFilters.push.apply(allFacetFilters, $scope.hiddenFilters);
        }
        updateSearch(allFacetFilters, force);
      };

      //on search completed
      $scope.onSearchCompleted = function(searchResult) {
        if(_.undefined(searchResult.error)) {
          // inject static facets
          if(_.defined($scope.staticFacets)) {
            _.each($scope.staticFacets, function(facet, facetKey){
              searchResult.data.facets[facetKey] = facet;
            });
          }

          // inject selecte version for each result
          _.each(searchResult.data.data, function(component){
            component.selectedVersion = component.archiveVersion;
          });
          $scope.searchResult = searchResult.data;
          $scope.detailComponent = null;
        } else {
          console.error('error when searching...', searchResult.error);
        }
      };

      var versionFetchResource = $alresource('rest/latest/components/element/:elementId/versions');
      $scope.fetchElementVersion = function(component) {
        if(_.defined(component.olderVersions)) {
          return;
        }
        versionFetchResource.get({elementId: component.elementId, toscaType: $scope.queryComponentType},function(result) {
          if(_.defined(result.error)) {
            console.error('Encountered error while fetching element versions', component.elementId, result.error);
          } else {
            component.olderVersions = result.data;
          }
        });
      };

      // Getting full search result from /data folder


      /*Remove a facet filter*/
      $scope.removeFilter = function(filterToRemove) {
        // Remove the selected filter
        _.remove($scope.facetFilters, filterToRemove);
        // Search update with new filters list
        $scope.doSearch();
      };

      /*Reset all filters*/
      $scope.reset = function() {
        // Reset all filters
        $scope.facetFilters.splice(0, $scope.facetFilters.length);
        $scope.doSearch();
      };

      /** check if this component is default for a capability */
      $scope.isADefaultCapability = function(component, capability) {
        if (component.defaultCapabilities) {
          return (component.defaultCapabilities.indexOf(capability) >= 0);
        }
      };

      /* Restrict tags visibility */
      $scope.isInternalTag = function(tag) {
        return alienInternalTags.indexOf(tag) >= 0;
      };

      $scope.handleItemSelection = function(item) {
        if (!$scope.dragAndDropEnabled) {
          $scope.onSelectItem({
            item: item
          });
        }
      };

      //watch the bound data
      $scope.$watch('refresh', function() {
        $scope.doSearch(true);
      });

      $scope.heightStyle = function() {
        if ($scope.globalContext) {
          return {
            overflow: 'auto',
            height: $scope.height + 'px'
          };
        }
        return {
          overflow: 'auto'
        };
      };

      //get the icon
      $scope.getIcon = toscaService.getIcon;

      var componentResource = $alresource('rest/latest/components/:id');
      $scope.selectOtherComponentVersion = function(component, newVersion, index, event) {
        if (event) {
          event.stopPropagation();
        }
        component.selectedVersion = newVersion;
        if (component.archiveVersion !== newVersion) {
          // Retrieve the other version
          componentResource.get({
            id: newVersion.id
          }, function(successResult) {
            // override the component with the retrieved version
            var selectedVersionComponent = successResult.data;
            selectedVersionComponent.olderVersions = component.olderVersions;
            selectedVersionComponent.selectedVersion = newVersion.version;
            $scope.searchResult.data[index] = selectedVersionComponent;
          });
        }
      };

      $scope.handleBadgeClick = function(badge, component, event) {
        if(_.isFunction(badge.onClick)){
          if(event){
            event.stopPropagation();
          }
          badge.onClick(component, $state);
        }
      };

    }]); // controller
}); // define
