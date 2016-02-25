define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  modules.get('a4c-components', ['ngResource']).factory('componentTagService', ['$resource', function($resource) {
    // API REST Definition
    var updateRes = $resource('rest/latest/components/:componentId/tags', {}, {
      'upsert': {
        method: 'POST',
        isArray: false,
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });
    var removeRes = $resource('rest/latest/components/:componentId/tags/:tagKey', {}, {
      'remove': {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json; charset=UTF-8'
        }
      }
    });
    return {
      upsert: updateRes.upsert,
      remove: removeRes.remove
    };
  }]);
});
