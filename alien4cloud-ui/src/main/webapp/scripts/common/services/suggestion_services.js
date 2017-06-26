define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common').factory('suggestionServices', ['$resource', function($resource) {
    var tagSuggestionResource = $resource('rest/latest/suggest/tag/:path/:searchText', {}, {
      'get' : {
        method : 'GET'
      }
    });

    var nodetypeSuggestionResource = $resource('rest/latest/suggest/nodetypes');

    var getTagNameSuggestions = function(keyword) {
      return tagSuggestionResource.get({
        path : 'name',
        searchText : keyword
      }).$promise.then(function(result) {
        var formatedData = result.data;
        formatedData.sort();
        return formatedData;
      });
    };

    var getNodetypeSuggestions = function(keyword) {
      return nodetypeSuggestionResource.get({
        text : keyword
      }).$promise.then(function(result) {
        return result.data;
      });
    };
  
    var getAbstractNodetypeSuggestions = function(keyword) {
      return nodetypeSuggestionResource.get({
        text : keyword,
        isAbstract: true
      }).$promise.then(function(result) {
        return result.data;
      });
    };

    return {
      tagNameSuggestions : getTagNameSuggestions,
      nodetypeSuggestions : getNodetypeSuggestions,
      abstractNodetypeSuggestions : getAbstractNodetypeSuggestions
    };

  }]); // factory
}); // define
