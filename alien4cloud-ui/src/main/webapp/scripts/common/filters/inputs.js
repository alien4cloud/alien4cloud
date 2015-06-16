/** Filter inputs by id (check CONSTANTS.js / excludedInputs) */
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common').filter('allowedInputs', function() {
    return function(items) {
      var filtered = {};
      var foundStart = false;
      angular.forEach(items, function(inputValue, inputId) {
        CONSTANTS.excludedInputs.forEach(function(inputStart) {
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
