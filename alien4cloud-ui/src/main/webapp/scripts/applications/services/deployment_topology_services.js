define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications', ['ngResource']).factory('deploymentTopologyServices', ['$resource', '$alresource',
    function($resource, $alresource) {
      var location = $resource('rest/v1/applications/:appId/environments/:envId/deployment-topology/location-policies', {}, {
        setLocationPolicies: {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      var deploymentTopology = $alresource('rest/v1/applications/:appId/environments/:envId/deployment-topology');

      var nodeSubstitution = $resource('rest/v1/applications/:appId/environments/:envId/deployment-topology/substitutions/:nodeId');

      var nodeSubstitutionProperty = $resource('rest/v1/applications/:appId/environments/:envId/deployment-topology/substitutions/:nodeId/properties');

      var nodeSubstitutionCapabilityProperty = $resource('rest/v1/applications/:appId/environments/:envId/deployment-topology/substitutions/:nodeId/capabilities/:capabilityName/properties');

      return {
        'setLocationPolicies': location.setLocationPolicies,
        'get': deploymentTopology.get,
        'getAvailableSubstitutions': nodeSubstitution.get,
        'updateSubstitution': nodeSubstitution.save,
        'updateSubstitutionProperty': nodeSubstitutionProperty.save,
        'updateSubstitutionCapabilityProperty': nodeSubstitutionCapabilityProperty.save,
        'updateInputProperties': deploymentTopology.update
      };
    }
  ]);
});
