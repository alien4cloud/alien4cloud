define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications', ['ngResource']).factory('deploymentConfigurationServices', ['$resource',
    function($resource) {
      var deploymentConfigurer = $resource('rest/applications/:appId/deployment/configure', {}, {
        update: {
          method: 'PUT',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      return {
        'update': deploymentConfigurer.update
      };
    }
  ]);
});
