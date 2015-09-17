define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications', ['ngResource']).factory('deploymentTopologyServices', ['$resource',
    function($resource) {
      var location= $resource('rest/applications/:appId/environments/:envId/deployment-topology/location-polocies', {}, {
        setLocationPolicies: {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });
      
      var deploymentTopology= $resource('rest/applications/:appId/environments/:envId/deployment-topology', {}, {
        get: {
          method: 'GET'
        }
      });
      
      var deploymentTopologyInit= $resource('rest/applications/:appId/environments/:envId/deployment-topology/init', {}, {
        get: {
          method: 'GET'
        }
      });

      return {
        'setLocationPolicies': location.setLocationPolicies,
        'get': deploymentTopology.get,
        'init': deploymentTopologyInit.get
      };
    }
  ]);
});
