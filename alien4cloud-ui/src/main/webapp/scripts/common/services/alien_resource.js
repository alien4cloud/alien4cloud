// Simplify the generation of crud resources for alien
// list of cloud images that can be defined for multiple clouds actually.
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common', ['ngResource']).factory('$alresource', ['$resource',
    function($resource) {
      var headers = {
        'Content-Type': 'application/json; charset=UTF-8'
      };
      return function(url) {
        return $resource(url, {}, {
          'create': {
            method: 'POST',
            isArray: false,
            headers: headers
          },
          'get': {
            method: 'GET',
            isArray: false,
            headers: headers
          },
          'update': {
            method: 'PUT',
            isArray: false,
            headers: headers
          },
          'remove': {
            method: 'DELETE',
            isArray: false,
            headers: headers
          }
        });
      };
    }
  ]);
});
