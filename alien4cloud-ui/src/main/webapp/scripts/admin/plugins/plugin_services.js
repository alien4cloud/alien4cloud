define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-plugins', ['ngResource']).factory('pluginServices', ['$location', '$resource',
    function($location, $resource) {

      //add / remove roles
      var manageConfigs = $resource('rest/latest/plugins/:pluginId/config', {}, {
        'get': {
          method: 'GET'
        },
        'save': {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var pluginResource = $resource('rest/latest/plugins/:pluginId', {}, {
        'get': {
          method: 'GET'
        },
        'remove': {
          method: 'DELETE'
        }
      });

      return {
        'get': pluginResource.get,
        'remove': pluginResource.remove,
        'config': {
          'get': manageConfigs.get,
          'save': manageConfigs.save
        }
      };
    }
  ]);
});
