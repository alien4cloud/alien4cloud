define(function (require) {
  'use strict';

  var angular = require('angular');
  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/tosca/services/tosca_service');

  modules.get('a4c-components', ['a4c-tosca']).controller('alienSearchComponentCtrl', ['$scope', '$filter', 'searchContext', '$resource', 'toscaService', 'searchServiceFactory', function($scope, $filter, searchContext, $resource, toscaService, searchServiceFactory) {
    var alienInternalTags = ['icon'];

    $scope.searchService = searchServiceFactory('rest/components/search', false, $scope, 20, 10);
    $scope.searchService.filtered(true);

    /** Used to display the correct text in UI */
    $scope.getFormatedFacetValue = function(term, value) {
      // Add other boolean term facet in the condition
      if (term === 'abstract') {
        if (value === 'F' || value[0] === false) {
          return $filter('translate')('FALSE');
        } else {
          return $filter('translate')('TRUE');
        }
      } else {
        return value;
      }
    };

    /** Used to send the correct request to ES */
    function getFormatedFacetId(term, facetId) {
      // Add other boolean term facet in the condition
      if (term === 'abstract') {
        if (facetId === 'F') {
          return false;
        } else {
          return true;
        }
      } else {
        return facetId;
      }
    }

    $scope.setComponent = function(component) {
      $scope.detailComponent = component;
    };

    /**
     * search handlers
     */
    //bind the scope search vars to the searchContext service
    if ($scope.globalContext) {
      $scope.query = searchContext.query;
      $scope.facetFilters = searchContext.facetFilters;
    } else {
      $scope.query = '';
      $scope.facetFilters = [];
    }

    /*update a search*/
    function updateSearch(filters) {
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
      $scope.searchService.search(null, searchRequestObject);
    }

    /*trigger a new search, when params are changed*/
    $scope.doSearch = function() {
      var allFacetFilters = [];
      allFacetFilters.push.apply(allFacetFilters, $scope.facetFilters);
      if (angular.isDefined($scope.hiddenFilters)) {
        allFacetFilters.push.apply(allFacetFilters, $scope.hiddenFilters);
      }
      updateSearch(allFacetFilters);
    };

    //on search completed
    $scope.onSearchCompleted = function(searchResult) {
      if(_.undefined(searchResult.error)) {
        // inject selecte version for each result
        _.each(searchResult.data.data, function(component){
          component.selectedVersion = component.archiveVersion;
          if(_.undefined(component.olderVersions)) {
            component.olderVersions = [];
          }
          component.olderVersions.splice(0, 0, component.archiveVersion);
        });
        $scope.searchResult = searchResult.data;
        $scope.detailComponent = null;
      } else {
        console.log('error when searching...', searchResult.error);
      }
    };

    // Getting full search result from /data folder

    /* Add a facet Filters*/
    $scope.addFilter = function(termId, facetId) {

      // Test if the filter exists : [term:facet]
      var termIndex = -1;
      for (var i = 0, len = $scope.facetFilters.length; i < len; i++) {
        if ($scope.facetFilters[i].term === termId && $scope.facetFilters[i].facet === facetId) {
          termIndex = i;
        }
      }

      if (termIndex < 0) {
        var facetSearchObject = {};
        facetSearchObject.term = termId;
        facetSearchObject.facet = [];
        facetSearchObject.facet.push(getFormatedFacetId(termId, facetId));
        $scope.facetFilters.push(facetSearchObject);
      }

      // Search update with new filters list
      $scope.doSearch();
    };

    /*Remove a facet filter*/
    $scope.removeFilter = function(filterToRemove) {

      // Remove the selected filter
      var index = $scope.facetFilters.indexOf(filterToRemove);
      if (index >= 0) {
        $scope.facetFilters.splice(index, 1);
      }

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
      $scope.doSearch();
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

    var componentResource = $resource('rest/components/:componentId', {}, {
      method: 'GET',
      isArray: false,
      headers: {
        'Content-Type': 'application/json; charset=UTF-8'
      }
    });

    $scope.selectOtherComponentVersion = function(component, newVersion, index, event) {
      if (event) {
        event.stopPropagation();
      }
      component.selectedVersion = newVersion;
      if (component.archiveVersion !== newVersion) {
        // Retrieve the other version
        componentResource.get({
          componentId: component.elementId + ':' + newVersion
        }, function(successResult) {
          // override the component with the retrieved version
          var selectedVersionComponent = successResult.data;
          selectedVersionComponent.olderVersions = component.olderVersions;
          selectedVersionComponent.selectedVersion = newVersion;
          $scope.searchResult.data[index] = selectedVersionComponent;
        });
      }
    };

  }]); // controller
}); // define
