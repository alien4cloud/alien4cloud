'use strict';

angular.module('alienUiApp').factory('browserService',
  function() {
    return {
      /**
      * Check if the current browser is the candidate browser (from it's name).
      *
      * @param The name of the browser to check.
      * @return true if the current browser is an instance of the requested browser.
      */
      isBrowser: function(candidateBrowserName) {
        return navigator.userAgent.indexOf(candidateBrowserName) !== -1;
      }
    };
  } // function
);
