define(function(require) {
  'use strict';

  var modules = require('modules');
  // var states = require('states');
  // var _ = require('lodash');

  // modules.get('a4c-auth', ['ngResource']).factory('languages', ['$resource', '$location', '$state', '$http',
  //   function($resource, $location, $state, $http) {


  modules.get('a4c-common', ['ngResource']).factory('languages', ['$resource',
    function($resource) {
      var defaultLanguage = $resource('rest/latest/languages/default', {}, {
        'query': {
          method: 'GET',
          isArray: false
        }
      });
      // get default 'all-users' group
      var languageFilePrefix = $resource('rest/latest/languages/prefix', {}, {
        'query': {
          method: 'GET',
          isArray: false
        }
      }).query();

      return {
        'defaultLanguage': defaultLanguage,
        'languageFilePrefix': languageFilePrefix
      };

    }
  ]);
});
