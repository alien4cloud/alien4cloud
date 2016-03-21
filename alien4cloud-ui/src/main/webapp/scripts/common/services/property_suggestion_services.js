define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common').factory('propertySuggestionServices', ['$resource', function($resource) {

    var propertySuggestionResource = $resource('rest/latest/suggestions/:suggestionId/values', {}, {
      'get': {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        },
        params: {
          input: '@input',
          limit: '@limit'
        }
      }
    });

    var propertySuggestionPutValueResource = $resource('rest/latest/suggestions/:suggestionId/values/:value', {}, {
      'update': {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        },
        params: {
          suggestionId: '@suggestionId',
          value: '@value'
        }
      }
    });


    return {
      get: propertySuggestionResource.get,
      add: propertySuggestionPutValueResource.update
    };

  }]); // factory
}); // define
