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
