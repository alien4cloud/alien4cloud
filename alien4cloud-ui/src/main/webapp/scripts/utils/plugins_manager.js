// Utility module that query a4c to get the base url of a given plugin.
define(function (require) {
  'use strict';
  
  var $ = require('jquery');
  var _ = require('lodash');

  return {
    init: function() {
      var self = this;
      var deferred = $.Deferred();
      $.ajax({ url: '/rest/modules' }).then(function(data) {
        self.plugins = data;
        // init returns the list of entry points for the plugins.
        var entryPoints = [];
        _.each(self.plugins, function(value){
          entryPoints.push(value.entryPoint);
        });
        deferred.resolve(entryPoints);
      });
      return deferred;
    },
    base: function(pluginName) {
      if(_.defined(this.plugins[pluginName])) {
        return this.plugins[pluginName].base;
      }
      return null;
    }
  };
});
