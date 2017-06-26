define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var GROUP_ALL = '_A4C_ALL';


  modules.get('a4c-applications').factory('deploymentContextUtils',
    [ function() {
        return {

          formatLocationMatches: function (scope, locationMatches) {
            scope.deploymentContext.locationMatches = {};
            _.each(locationMatches, function(locationMatch) {
              scope.deploymentContext.locationMatches[locationMatch.location.id] = locationMatch;
              locationMatch.selected = false;
            });
          },

          initSelectedLocation: function (scope) {
            delete scope.deploymentContext.selectedLocation;
            if (_.has(scope, 'deploymentContext.deploymentTopologyDTO.locationPolicies.' + GROUP_ALL)) {
              var selectedLocationId = scope.deploymentContext.deploymentTopologyDTO.locationPolicies[GROUP_ALL];
              if (scope.deploymentContext.locationMatches && scope.deploymentContext.locationMatches[selectedLocationId]) {
                scope.deploymentContext.selectedLocation = scope.deploymentContext.locationMatches[selectedLocationId].location;
                scope.deploymentContext.locationMatches[selectedLocationId].selected = true;
              }
            }
          }
        };
      } // function
    ]); // factory
});
