define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications', ['ngResource']).factory('deploymentTopologyServices', ['$resource',
    function($resource) {
      var location = $resource('rest/applications/:appId/environments/:envId/deployment-topology/location-policies', {}, {
        setLocationPolicies: {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var deploymentTopology = $resource('rest/applications/:appId/environments/:envId/deployment-topology');

      var nodeSubstitution = $resource('rest/applications/:appId/environments/:envId/deployment-topology/substitutions/:nodeId');

      return {
        'setLocationPolicies': location.setLocationPolicies,
        'get': deploymentTopology.get,
        'getAvailableSubstitutions': nodeSubstitution.get,
        'updateSubstitution': nodeSubstitution.save
      };
    }
  ]);
});
