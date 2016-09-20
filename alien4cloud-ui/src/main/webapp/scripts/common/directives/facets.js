define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common', []).directive('facets', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/common/facets.html',
      controller: 'FacetsController',
      scope: {
        // currently selected filters by user
        facetFilters: '=',
        // facet data, which can be updated vi doSearch()
        facets: '=',
        // the prefix for all label, it's useful for translation
        filterPrefix: '@',
        // trigger a query to refresh data
        doSearch: '&'
      }
    };
  });

  modules.get('a4c-common', []).controller('FacetsController', ['$scope', function ($scope) {

    function addFacetFilter(termId, facetId) {
      // Test if the filter exists : [term:facet] and add it if not
      var filter = _.find($scope.facetFilters, {term: termId});
      if (_.undefined(filter)) {
        var facetSearchObject = {};
        facetSearchObject.term = termId;
        facetSearchObject.facet = [];
        facetSearchObject.facet.push(facetId);
        $scope.facetFilters.push(facetSearchObject);
      } else {
        var index = _.indexOf(filter.facet, facetId);
        if(index === -1) {
          filter.facet.push(facetId);
        } else {
          _.pullAt(filter.facet, index);
          if(filter.facet.length === 0) {
            _.remove($scope.facetFilters, {term: termId});
          }
        }
      }
    }

    // Getting full search result from /data folder

    /* Add a facet Filters*/
    $scope.addFilter = function (termId, facetId) {
      addFacetFilter(termId, facetId);
      // Search update with new filters list
      $scope.doSearch();
    };

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

  }]);
});
