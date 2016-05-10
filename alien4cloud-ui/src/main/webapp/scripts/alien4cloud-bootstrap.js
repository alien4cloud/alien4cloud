define(function (require) {
  'use strict';

  // require jquery and load plugins from the server
  var plugins = require('plugins');
  var alien4cloud = require('alien4cloud');

  return {
    startup: function() {
      // load all plugins and then start alien 4 cloud.
      plugins.init().then(function() {
        alien4cloud.startup();
      });
    }
  };
});
