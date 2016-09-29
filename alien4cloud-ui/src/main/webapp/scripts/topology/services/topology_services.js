// define the rest api elements to work with topology edition.
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-editor', ['ngResource']).factory('topologyServices', ['$resource',
    function($resource) {
      // Service that gives access to create topology
      var topologyDAO = $resource('rest/latest/topologies/:topologyId', {}, {
        'create': {
          method: 'POST'
        },
        'get': {
          method: 'GET'
        }
      });
      var isValid = $resource('rest/latest/topologies/:topologyId/isvalid', {}, {
        method: 'GET'
      });

      return {
        'dao': topologyDAO,
        'isValid': isValid.get,
      };
    }
  ]);
}); // define
