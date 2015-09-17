define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications', ['ngResource']).factory('locationsMatchingServices', ['$resource',
    function($resource) {
      var locationsMatcher = $resource('rest/topology/:topologyId/locations', {}, {
        'getLocationsMatches': {
          method: 'GET'
        }
      });

      return {
        'getLocationsMatches': locationsMatcher.getLocationsMatches
      };
    }
  ]);
});
