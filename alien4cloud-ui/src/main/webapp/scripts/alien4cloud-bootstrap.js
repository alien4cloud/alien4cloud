define(function (require) {
  'use strict';

  // require jquery and load plugins from the server
  var plugins = require('plugins');
  var mods = {
    'nativeModules': require('a4c-native')
  };
  var alien4cloud = require('alien4cloud');

  return {
    startup: function() {
      //some common directives directives
      require(mods.nativeModules , function() {
        window.alienLoadingBar.className = 'progress-bar progress-bar-success';
        window.alienLoadingBar = undefined;
        window.alienLoadingFile = undefined;

        // load all plugins and then start alien 4 cloud.
        plugins.init().then(function() {
          alien4cloud.startup();
        });
      });
    }
  };
});
