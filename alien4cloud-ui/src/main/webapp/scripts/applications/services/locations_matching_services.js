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
            locationMatch.selected = false;
          });
          // Process with location selection
          scope.selectedLocations = [];

          _.each(scope.deploymentTopologyDTO.locationPolicies, function(value, key) {
            var selectedLocationId = value;
            if (scope.locationMatches && scope.locationMatches[selectedLocationId]) {
              scope.locationMatches[selectedLocationId].selected = true;
              scope.selectedLocations.push(scope.locationMatches[selectedLocationId].location)
            }
          });
        }
      };
    }
  ]);
});
