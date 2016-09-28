/* global define */
'use strict';

// Helper object to manage modules
define(function (require) {
  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  modules.get('a4c-search').factory('searchServiceFactory', ['$resource', function($resource) {
    var create = function(url, useParam, queryProvider, maxSearchSize, maxPageNumbers, isPaginatedAPI, paramsConfig, params, body) {
      if(_.undefined(isPaginatedAPI)) {
        isPaginatedAPI = true;
      }
      if (!maxSearchSize) {
        maxSearchSize = 20;
      }
      if (!maxPageNumbers) {
        maxPageNumbers = 10;
      }
      var isFiltered=false;
      var resource;
      if (useParam) {
        // If useParam is true the search service expects an API that does not accept request body but search parameters as url params
        var searchParams = {
          query: '@query',
          from: '@from',
          size: '@size'
        };

        if (_.defined(paramsConfig)) {
          searchParams = _.merge(paramsConfig, searchParams);
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

      var pagination = {
        selectPage: function(page) {
          this.currentPage = page;
          this.from = (page - 1) * this.maxSearchSize;
          search(this.from, this.searchContext.additionalBody, true); // use cache if any
        },
        update: function(searchResult) {
          this.totalItems = searchResult.data.totalResults;
        },
        reset: function() {
          this.from = 0;
          this.currentPage = 1;
        },
        maxSearchSize: maxSearchSize,
        maxPageNumbers: maxPageNumbers,
        totalItems: 0,
        searchContext:{}
      };

      // holds the results of the last query
      var cachedResult = {
        queryParams: {},
        queryBody: {},
        searchResult: {}
      };
      var setCachedResult = function(queryParams, queryBody, searchResult) {
        cachedResult = {
          queryParams: queryParams,
          queryBody: queryBody,
          searchResult: searchResult
        };
      };
      var isCachedQuery = function(queryParams, queryBody) {
        return _.isEqual(cachedResult.queryParams, queryParams) && _.isEqual(cachedResult.queryBody, queryBody);
      };

      var setResults= function(from) { // set the results
        var searchResult = cachedResult.searchResult;
        if(!isPaginatedAPI) {
          searchResult = _.clone(cachedResult.searchResult); // no need for a deep clone
          searchResult.from = from;
          var count = from + maxSearchSize;
          if(count > searchResult.data.length) {
            count = searchResult.data.length;
          }
          searchResult.data = {
            data: cachedResult.searchResult.data.data.slice(from, count),
            facets: cachedResult.searchResult.data.facets,
            from: from,
            queryDuration: 0,
            to: cachedResult.searchResult.data.to,
            totalResults: cachedResult.searchResult.data.totalResults,
            types: cachedResult.searchResult.data.types.slice(from, count)
          };
        }
        pagination.update(searchResult);
        queryProvider.onSearchCompleted(searchResult);
      };

      var search = function(from, additionalBody, useCache) {
        if (!from) {
          pagination.reset();
          from = pagination.from;
        }

        var baseQuery = {
          query: queryProvider.query,
          from: from,
          size: maxSearchSize
        };

        if (useParam) { // URL parameters based query
          var searchRequest = baseQuery;
          if (_.defined(params)) {
            searchRequest = _.merge(params, searchRequest);
          }
          resource.search(searchRequest, function(searchResult) {
            setCachedResult(searchRequest, undefined, searchResult);
            setResults(from);
          });
        } else { //
          var searchBody = baseQuery;
          if (_.defined(body)) {
            searchBody = _.merge(body, searchBody);
          }
          if(_.defined(additionalBody)){
            searchBody = _.merge(searchBody, additionalBody);
            pagination.searchContext.additionalBody = additionalBody;
          }
          if(isFiltered){
            searchBody.filters=queryProvider.filters;
          }

          if(!isPaginatedAPI) { // mimic pagination from ui side.
            searchBody.from = 0;
            searchBody.size = maxSearchSize * maxPageNumbers;
          }

          if(useCache && isCachedQuery(params, searchBody)) {
            setResults(from);
            return;
          }

          resource.search(params, angular.toJson(searchBody), function(searchResult) {
            setCachedResult(params, searchBody, searchResult);
            setResults(from);
          });
        }
      };

      return  {
        'pagination': pagination,
        'search': search,
        filtered: function(filtered) {
          // setter for the filtered configuration
          if(_.defined(filtered)){
            isFiltered = filtered;
          } else {
            isFiltered = false;
          }
        }
      };
    };

    return create;
  }]);
});
