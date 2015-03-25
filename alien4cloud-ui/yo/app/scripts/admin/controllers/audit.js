'use strict';

angular.module('alienUiApp').controller('AuditController', ['$scope', 'auditService', 'ngTableParams', '$filter', '$timeout',
  function($scope, auditService, ngTableParams, $filter, $timeout) {

    // display configuration
    var timestampFormat = 'medium';

    // displayed column
    $scope.columns = [{
      title: 'Date',
      field: 'timestamp',
      visible: true
    }, {
      title: 'Username',
      field: 'userName',
      visible: true
    }, {
      title: 'Category',
      field: 'category',
      visible: true
    }, {
      title: 'Action',
      field: 'action',
      visible: true
    }, {
      title: 'Method',
      field: 'method',
      visible: true
    }, {
      title: 'Response status',
      field: 'responseStatus',
      visible: true
    }];

    //////////////////////////////////
    // UI utils methods
    //////////////////////////////////

    // format traces befor display
    function prepareTraces(traces) {
      // prepare each "timestamp" field
      traces.forEach(function(trace) {
        trace.timestamp = $filter('date')(trace.timestamp, timestampFormat);
      });
    }

    // use to display the correct text in UI
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

    //////////////////////////////////
    // Search methods
    //////////////////////////////////
    var doSearch = function() {
      // prepare filters
      var allFacetFilters = [];
      allFacetFilters.push.apply(allFacetFilters, $scope.facetFilters);
      if (angular.isDefined($scope.hiddenFilters)) {
        allFacetFilters.push.apply(allFacetFilters, $scope.hiddenFilters);
      }
      updateSearch($scope.searchedKeyword, allFacetFilters);
    };

    // initial search
    doSearch();
    $scope.doSearch = doSearch;

    // update search result table
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
        'query': keyword,
        'filters': objectFilters,
        'from': 0,
        'size': 1000
      };

      console.log('Query OBJECT >', searchRequestObject);

      // recover audit search results
      $scope.rows = auditService.search([], angular.toJson(searchRequestObject), function(successResult) {
        // get facets
        $scope.facets = successResult.data.facets;
        return successResult.data.data;
      });

      $scope.rows.$promise.then(function handleData(rows) {
        var data = rows.data.data;
        prepareTraces(data);
        // configure the ng-table
        $scope.auditTableParam = new ngTableParams({
          page: 1, // show first page
          count: data.length // count per page
        }, {
          total: data.length, // length of data
          getData: function($defer, params) {
            $defer.resolve(data.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
        });
        // console.log('RELOAD THE DATA >', data, data.length, $scope.auditTableParam, $scope.auditTableParam.total());

      });
    }

    /* Add a facet Filters*/
    $scope.addFilter = function(termId, facetId) {
      console.log('add filter >', termId, facetId);
      $scope.facetFilters = $scope.facetFilters || [];
      // Test if the filter exists : [term:facet]
      var termIndex = -1;
      for (var i = 0, len = $scope.facets.length; i < len; i++) {
        if ($scope.facetFilters[i].term === termId && $scope.facetFilters[i].facet === facetId) {
          termIndex = i;
        }
      }

      if (termIndex < 0) {
        var facetSearchObject = {};
        facetSearchObject.term = termId;
        facetSearchObject.facet = [];
        facetSearchObject.facet.push(facetId);
        $scope.facetFilters.push(facetSearchObject);
      }

      // Search update with new filters list
      $scope.doSearch();
    };

    /*Remove a facet filter*/
    $scope.removeFilter = function(filterToRemove) {
      console.log('Remove filter >', filterToRemove);
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


  }
]);
