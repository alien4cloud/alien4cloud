define(function(require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-applications', ['ngResource']).factory('policiesMatchingService', ['$resource', '$alresource',
    function($resource) {

      var policySubstitution = $resource('rest/latest/applications/:appId/environments/:envId/deployment-topology/policies/:nodeId/substitution');

      var policySubstitutionProperty = $resource('rest/latest/applications/:appId/environments/:envId/deployment-topology/policies/:nodeId/substitution/properties');

      return {
        'getAvailableSubstitutions': policySubstitution.get,
        'updateSubstitution': policySubstitution.save,
        'updateSubstitutionProperty': policySubstitutionProperty.save,
      };
    }
  ]);
});
