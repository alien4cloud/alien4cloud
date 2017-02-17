// Simplify the generation of crud resources for alien
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common', ['ngResource']).factory('$alresource', ['$resource',
    function($resource) {
      var headers = {
        'Content-Type': 'application/json; charset=UTF-8'
      };
      return function(url, operations, params) {
        var targetOperations = {
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
          'patch': {
            method: 'PATCH',
            isArray: false,
            headers: headers
          },
          'remove': {
            method: 'DELETE',
            isArray: false,
            headers: headers
          }
        };

        if(_.defined(operations)) {
          _.merge(targetOperations, operations);
        }
        return $resource(url, _.defined(params)? params : {}, targetOperations);
      };
    }
  ]);
});
