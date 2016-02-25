define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-components', ['ngResource']).factory('componentService', ['$http', '$resource', function($http, $resource) {
    var componentResource = $resource('rest/latest/components/:componentId', {}, {
      'get': {
        method: 'GET'
      }
    });
    return {
      'get': componentResource.get,
      'getInArchives': function(elementName, componentType, dependencies) {
        return $http.post('rest/latest/components/getInArchives', {
          'elementName': elementName,
          'componentType': componentType,
          'dependencies': dependencies
        });
      }
    };
  }]); // factory
}); // define
