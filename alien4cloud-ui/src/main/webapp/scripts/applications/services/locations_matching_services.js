define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-applications', ['ngResource']).factory('locationsMatchingServices', ['$resource',
    function($resource) {
      var locationsMatcher = $resource('rest/latest/topologies/:topologyId/locations', {}, {
        'getLocationsMatches': {
          method: 'GET'
        }
      });

      return {
        getLocationsMatches: locationsMatcher.getLocationsMatches,

        processLocationMatches: function (scope, locationMatches) {
          scope.locationMatches = {};
          // Initialize the scope location matches with
          _.each(locationMatches, function(locationMatch) {
            scope.locationMatches[locationMatch.location.id] = locationMatch;
          });
          // Process with location selection
          scope.selectedLocations = [];

          _.each(scope.deploymentTopologyDTO.topology.locationGroups, function(locationGrp, key) {
            if (locationGrp.policies) {
              var location = scope.locationMatches[locationGrp.policies[0].locationId].location
              if (! scope.selectedLocations.includes(location)) {
                scope.selectedLocations.push(location)
              }
            }
          });
        }
      };
    }
  ]);
});
