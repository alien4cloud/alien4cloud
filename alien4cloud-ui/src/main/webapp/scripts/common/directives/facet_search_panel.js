define(function (require) {
  'use strict';

  require('scripts/common/directives/facets');

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common', []).directive('facetSearchPanel', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/common/facet_search_panel.html',
      controller: 'FacetSearchPanelController',
      scope: {
        searchUrl: '@',
        componentPrefix: '@',
        onSearch: '&'
      }
    };
  });

  modules.get('a4c-common', []).controller('FacetSearchPanelController', ['$scope', 'searchServiceFactory', function ($scope, searchServiceFactory) {

    $scope.facetFilters = [];

    /*update a search*/
    function updateSearch(filters) {
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
      $scope.queryProvider.filters = objectFilters;
      $scope.searchService.search();
    }

    /*trigger a new search, when params are changed*/
    $scope.doSearch = function () {
      updateSearch($scope.facetFilters);
    };
    //on search completed
    var onSearchCompleted = function (searchResult) {
      if (_.undefined(searchResult.error)) {
        $scope.facets = searchResult.data.facets;
        $scope.onSearch({
          searchConfig: {
            result: searchResult.data,
            service: $scope.searchService
          }
        });
      } else {
        console.log('error when searching...', searchResult.error);
      }
    };

    $scope.queryProvider = {
      query: '',
      onSearchCompleted: onSearchCompleted
    };

    $scope.searchService = searchServiceFactory($scope.searchUrl, false, $scope.queryProvider, 10, 10);
    $scope.searchService.filtered(true);
    $scope.doSearch();

  }]);
});