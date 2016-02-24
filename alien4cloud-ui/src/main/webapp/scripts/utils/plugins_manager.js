// Utility module that query a4c to get the base url of a given plugin.
define(function (require) {
  'use strict';

  var $ = require('jquery');
  var _ = require('lodash');

  return {
    init: function() {
      var self = this;
      var deferred = $.Deferred();
      $.ajax({ url: '/rest/v1/modules' }).then(function(data) {
        self.plugins = data;
        // init returns the list of entry points for the plugins.
        var entryPoints = [];
        var pluginNames = [];
        _.each(self.plugins, function(value, key){
          // load the plugins entry points.
          entryPoints.push(value.entryPoint);
          pluginNames.push(key);
        });
        deferred.resolve(entryPoints, pluginNames);
      });
      return deferred;
    },
    base: function(pluginName) {
      if(_.defined(this.plugins[pluginName])) {
        return this.plugins[pluginName].base;
      }
      return ''; // the plugin has no defined base
    }
  };
});
