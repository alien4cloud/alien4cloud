/* global UTILS */

'use strict';

angular.module('alienUiApp').controller('alienSearchCtrl', ['$scope', '$filter', 'facetedSearch', '$location', 'searchContext', 'alienAuthService', '$resource', function($scope, $filter, facetedSearch, $location, searchContext, alienAuthService, $resource) {
  var alienInternalTags = ['icon'];
  /**
   * pagination handlers
   */

  // pagination vars
  $scope.pagination = {};
  $scope.pagination.maxItemsPerPage = 20;
  $scope.pagination.maxSize = 10;

  /**
   * Use to display the correct text in UI
   */
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

  /**
   * Use to send the correct request to ES
   */
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

  //update paginations vars
  function updatePagination() {
    if(UTILS.isDefinedAndNotNull($scope.searchResult.data)) {
      $scope.pagination.totalItems = $scope.searchResult.data.totalResults;
    }
  }

  function resetPagination() {
    $scope.pagination.from = 0;
    $scope.currentPage = 1;
  }

  $scope.setComponent = function(component) {
    $scope.detailComponent = component;
  };


  /**
   * search handlers
   */

  //bind the scope search vars to the searchContext service
  if ($scope.globalContext) {
    $scope.searchedKeyword = searchContext.searchedKeyword;
    $scope.facetFilters = searchContext.facetFilters;
  } else {
    $scope.searchedKeyword = '';
    $scope.facetFilters = [];
  }


  /*update a search*/
  function updateSearch(keyword, filters) {

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
      'type': $scope.queryComponentType,
      'query': keyword,
      'filters': objectFilters,
      'from': $scope.pagination.from,
      'size': $scope.pagination.maxItemsPerPage
    };

    // Gather search result
    $scope.searchResult = facetedSearch.search([], angular.toJson(searchRequestObject));
    //wait for the asynchronous request to finish. If successful then...
    $scope.searchResult.$promise.then(updatePagination);
  }

  /*trigger a new search, when params are changed. Reset the pagination also*/
  $scope.doSearch = function() {
    resetPagination();
    var allFacetFilters = [];
    allFacetFilters.push.apply(allFacetFilters, $scope.facetFilters);
    if (angular.isDefined($scope.hiddenFilters)) {
      allFacetFilters.push.apply(allFacetFilters, $scope.hiddenFilters);
    }
    updateSearch($scope.searchedKeyword, allFacetFilters);
    // Handling facets
    $scope.searchResultFacets = $scope.searchResult.facets;
    $scope.detailComponent = null;
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

  //when selecting a page to display
  $scope.pagination.onSelectPage = function(page) {
    $scope.pagination.from = (page - 1) * $scope.pagination.maxItemsPerPage;
    updateSearch($scope.searchedKeyword, $scope.facetFilters);
    $scope.detailComponent = null;
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
  $scope.getIcon = UTILS.getIcon;

  var ComponentResource = $resource('rest/components/:componentId', {}, {
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
      ComponentResource.get({
        componentId: component.elementId + ':' + newVersion
      }, function(successResult) {
        var oldVersion = successResult.data;
        oldVersion.olderVersions = component.olderVersions;
        var indexOfOldVersion = oldVersion.olderVersions.indexOf(oldVersion.archiveVersion);
        oldVersion.olderVersions.splice(indexOfOldVersion, 1);
        oldVersion.olderVersions.push(component.archiveVersion);
        $scope.searchResult.data.data[index] = oldVersion;
      });
    }
  };

  // Init : by default, don't display abastract components on topology view
  if (!$scope.globalContext) {
    $scope.addFilter('abstract', 'F');
  }
}]);
