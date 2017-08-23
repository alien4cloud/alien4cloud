/** Filter inputs by id */
define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');

  var excludedInputs = ['loc_meta_', 'app_meta_', 'app_tags_', 'env_meta_', 'env_tags_'];

  modules.get('a4c-common').filter('internalInputs', function() {
    return function(items) {
      var filtered = {};
      var foundStart = false;
      angular.forEach(items, function(inputValue, inputId) {
        excludedInputs.forEach(function(inputStart) {
          // start by one reserved tag ?
          if (inputId.indexOf(inputStart) === 0) {
            foundStart = true;
          }
        });
        if (!foundStart) {
          filtered[inputId] = inputValue;
        }
        foundStart = false;
      });
      return filtered;
    };
  });
});
