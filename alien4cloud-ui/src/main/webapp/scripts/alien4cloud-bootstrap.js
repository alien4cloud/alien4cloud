define(function (require) {
  'use strict';
  
  // require jquery and load plugins from the server
  var plugins = require('plugins');
  var alien4cloud = require('alien4cloud');

  return {
    startup: function() {
      plugins.init().then(function(data) {
        // require modules
        require(data, function() {
          alien4cloud.startup();
        });
      });
    }
  };
});
