'use strict';

angular.module('searchServices', [ 'ngResource' ], ['$provide', function($provide) {
  $provide.factory('facetedSearch', ['$resource', function($resource) {
    // API REST Definition
    var resultsFacetedSearch = $resource('rest/components/search',
      {},
      {
        'search':
        {
          method:'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });
    return resultsFacetedSearch;
  }]);

  $provide.factory('searchContext', [function() {
    //The search context var
    var searchContext = {};

    //search query keyword: empty for the begining
    searchContext.searchedKeyword = '';

    //facets filters
    searchContext.facetFilters = [];

    return searchContext;
  }]);
}]);