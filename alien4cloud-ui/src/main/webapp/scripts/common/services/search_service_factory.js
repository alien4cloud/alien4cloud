/* global define */
'use strict';

// Helper object to manage modules
define(function (require) {
  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  modules.get('a4c-search').factory('searchServiceFactory', ['$resource', function($resource) {
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
          if (_.defined(params)) {
            searchRequest = _.merge(params, searchRequest);
          }
          resource.search(searchRequest, function(searchResult) {
            pagination.update(searchResult);
            queryProvider.onSearchCompleted(searchResult);
          });
        } else {
          var searchBody = baseQuery;
          if (_.defined(body)) {
            searchBody = _.merge(body, searchBody);
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
});
