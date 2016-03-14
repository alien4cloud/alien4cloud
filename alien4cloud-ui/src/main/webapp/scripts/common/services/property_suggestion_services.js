define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common').factory('propertySuggestionServices', ['$resource', function($resource) {

    var propertySuggestionResource = $resource('rest/latest/suggestions/:suggestionId');
    var propertySuggestionMachedValueResource = $resource('rest/latest/suggestions/:suggestionId/matched/:value');

    var getAllSuggestions = function(suggestionId) {
      return propertySuggestionResource.get({
        suggestionId : suggestionId
      }).$promise.then(function(result) {
        return result.data;
      });
    };

    var getMatchedSuggestions = function(suggestionId, value) {
      return propertySuggestionMachedValueResource.get({
        suggestionId : suggestionId,
        value : value
      }).$promise.then(function(result) {
        return result.data;
      });
    };

    var propertySuggestionPutValueResource = $resource('rest/latest/suggestions/:suggestionId/add/:value', {}, {
      'update': {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        },
        params: {
          suggestionId: '@suggestionId',
          value: '@value'
        }
      },
    });


    return {
      matchedSuggestions : getMatchedSuggestions,
      getAll : getAllSuggestions,
      add : propertySuggestionPutValueResource.update
    };

  }]); // factory
}); // define
