define(function (require) {
  'use strict';

  var modules = require('modules');

  var searchModule = modules.get('a4c-search', ['ngResource']);

  searchModule.factory('facetedSearch', ['$resource', function($resource) {
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

  searchModule.factory('searchContext', [function() {
    //The search context var
    var searchContext = {};

    //search query keyword: empty for the begining
    searchContext.query = '';

    //facets filters
    searchContext.facetFilters = [];

    return searchContext;
  }]);
});
