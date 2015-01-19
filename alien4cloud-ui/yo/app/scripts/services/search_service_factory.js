/* global UTILS */

'use strict';

angular.module('alienUiApp').factory('searchServiceFactory', ['$resource', function($resource) {
  var create = function(url, useParam, queryProvider, maxSearchSize, maxPageNumbers, paramsConfig, params, body) {
    if (!maxSearchSize) {
      maxSearchSize = 10;
    }
    if (!maxPageNumbers) {
      maxPageNumbers = 10;
    }
    var resource;
    if (useParam) {
      var searchParams = {
        query: '@query',
        from: '@from',
        size: '@size'
      };

      if (UTILS.isDefinedAndNotNull(paramsConfig)) {
        searchParams = UTILS.mergeObjects(paramsConfig, searchParams);
      }
      resource = $resource(url, {}, {
        'search': {
          method: 'GET',
          params: searchParams,
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });
    } else {
      resource = $resource(url, {}, {
        'search': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });
    }

    var update = function(searchResult) {
      this.totalItems = searchResult.data.totalResults;
    };

    var reset = function() {
      this.from = 0;
      this.currentPage = 1;
    };

    var selectPage = function(page) {
      this.currentPage = page;
      this.from = (page - 1) * this.maxSearchSize;
      search(this.from);
    };

    var pagination = {
      'selectPage': selectPage,
      'update': update,
      'reset': reset,
      'maxSearchSize': maxSearchSize,
      'maxPageNumbers': maxPageNumbers,
      'totalItems': 0
    };

    var search = function(from) {
      if (!from) {
        pagination.reset();
        from = pagination.from;
      }

      var baseQuery = {
        query: queryProvider.query,
        from: from,
        size: maxSearchSize
      };

      if (useParam) {
        var searchRequest = baseQuery;
        if (UTILS.isDefinedAndNotNull(params)) {
          searchRequest = UTILS.mergeObjects(params, searchRequest);
        }
        resource.search(searchRequest, function(searchResult) {
          pagination.update(searchResult);
          queryProvider.onSearchCompleted(searchResult);
        });
      } else {
        var searchBody = baseQuery;
        if (UTILS.isDefinedAndNotNull(body)) {
          searchBody = UTILS.mergeObjects(body, searchBody);
        }
        resource.search(params, angular.toJson(searchBody), function(searchResult) {
          pagination.update(searchResult);
          queryProvider.onSearchCompleted(searchResult);
        });
      }
    };

    return  {
      'pagination': pagination,
      'search': search
    };
  };

  return create;
}]);
