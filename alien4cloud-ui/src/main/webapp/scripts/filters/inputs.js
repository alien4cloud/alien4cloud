/* global CONSTANTS */
'use strict';

var alienApp = angular.module('alienUiApp');

/**
 * Filter a password with *
 */
alienApp.filter('password', function() {
  return function(password, car) {
    car = car || '*';
    var staredPassword = '';
    if (password !== null) {
      for (var i = 0; i < password.length; i++) {
        staredPassword += car;
      }
    }
    return staredPassword;
  };
});


/** Filter inputs by id (check CONSTANTS.js / excludedInputs) */
alienApp.filter('allowedInputs', function() {
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
