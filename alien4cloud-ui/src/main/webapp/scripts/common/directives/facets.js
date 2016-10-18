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
        /** default filters if any */
        defaultFilters: '=',
        // facet data, which can be updated vi doSearch()
        facets: '=',
        // the prefix for all label, it's useful for translation
        filterPrefix: '@',
        // trigger a query to refresh data
        doSearch: '&',
        // a converter that has toFilter(termId, facetId) and toDisplay(termId, facetId) operations to convert the values.
        facetIdConverter: '='
      }
    };
  });

  modules.get('a4c-common', []).controller('FacetsController', ['$scope', function ($scope) {
    function removeFilter(filterToRemove) {
      // Remove the selected filter
      _.remove($scope.facetFilters, filterToRemove);
      if(_.defined($scope.facets) && _.defined($scope.facets[filterToRemove.term]) && $scope.facets[filterToRemove.term].length >0 && _.defined($scope.facets[filterToRemove.term][0].staticFilter)) {
        // if the facet has a static filter it is a toggle kind of facet and cannot be removed.
        $scope.facetFilters.push( {term: filterToRemove.term, facet: _.clone($scope.facets[filterToRemove.term][0].staticFilter)});
      }
    }

    function addFacetFilter(termId, facetId) {
      if(_.defined($scope.facetIdConverter)) {
        facetId = $scope.facetIdConverter.toFilter(termId, facetId);
      }
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
            removeFilter({term: termId});
          }
        }
      }
    }

    if(_.defined($scope.defaultFilters)) {
      _.each($scope.defaultFilters, function(value, key) {
        var filter = _.find($scope.facetFilters, {term: key});
        if(_.undefined(filter)) {
          if(_.isArray(value)) {
            _.each(value, function(val) {
              addFacetFilter(key, val);
            });
          } else {
            addFacetFilter(key, value);
          }
        }
      });
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
      removeFilter(filterToRemove);
      // Search update with new filters list
      $scope.doSearch();
    };

    /*Reset all filters*/
    $scope.reset = function () {
      // Reset all filters
      $scope.facetFilters.splice(0, $scope.facetFilters.length);

      _.each($scope.facets, function(facet, key) {
        if(facet.length >0 && _.defined(facet[0].staticFilter)) {
          $scope.facetFilters.push( {term: key, facet: _.clone(facet[0].staticFilter)});
        }
      });

      $scope.doSearch();
    };

    $scope.toDisplay = function(termId, facetId) {
      if (_.defined($scope.facetIdConverter)) {
        return $scope.facetIdConverter.toDisplay(termId, facetId);
      }
      return facetId;
    };

    $scope.toDisplayFacet = function(termId, filterPrefix) {
      if (_.defined($scope.facetIdConverter)) {
        return $scope.facetIdConverter.toDisplayFacet(termId, filterPrefix);
      }
      return termId;
    };

    $scope.doSearch();
  }]);
});
