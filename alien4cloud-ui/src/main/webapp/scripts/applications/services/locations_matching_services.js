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
        GROUP_ALL: '_A4C_ALL',

        getLocationsMatches: locationsMatcher.getLocationsMatches,

        processLocationMatches: function (scope, locationMatches) {
          scope.locationMatches = {};
          // Initialize the scope location matches with
          _.each(locationMatches, function(locationMatch) {
            scope.locationMatches[locationMatch.location.id] = locationMatch;
            locationMatch.selected = false;
          });
          // Process with location selection
          delete scope.selectedLocation;
          if (_.has(scope, 'deploymentTopologyDTO.locationPolicies.' + this.GROUP_ALL)) {
            var selectedLocationId = scope.deploymentTopologyDTO.locationPolicies[this.GROUP_ALL];
            if (scope.locationMatches && scope.locationMatches[selectedLocationId]) {
              scope.selectedLocation = scope.locationMatches[selectedLocationId].location;
              scope.locationMatches[selectedLocationId].selected = true;
            }
          }
        }
      };
    }
  ]);
});
