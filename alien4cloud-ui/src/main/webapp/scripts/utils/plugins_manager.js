/* global requirejs */
// Utility module that query a4c to get the base url of a given plugin.
define(function (require) {
  'use strict';

  var $ = require('jquery');
  var _ = require('lodash');

  // out of require js as we want to overide the define function for plugins sandbox.
  var commentRegExp = /(\/\*([\s\S]*?)\*\/|([^:]|^)\/\/(.*)$)/mg,
    cjsRequireRegExp = /[^.]\s*require\s*\(\s*["']([^'"\s]+)["']\s*\)/g;

  return {
    registeredTranlations: [],
    init: function() {
      var self = this;
      var deferred = $.Deferred();

      document.getElementById('alien-plugins-loading-box').hidden = false;
      var loadingBar = document.getElementById('alien-plugins-loading-bar');
      var loadingName = document.getElementById('alien-plugins-loading-file');

      $.ajax({ url: '/rest/latest/modules', dataType : 'json', timeout : 10000, }).error(function(xhr, error) {
        loadingName.innerHTML = 'Failed to request plugins from server: ' + error;
        loadingBar.className = 'progress-bar progress-bar-danger';
        loadingBar.style.width = '100%';
      }).then(function(data) {
        self.plugins = data;
        // init returns the list of entry points for the plugins.
        var entryPoints = [];
        var pluginNames = [];
        var loadedPlugins = 0;
        var pluginCount = _.size(self.plugins);
        if(pluginCount === 0) {
          deferred.resolve();
        } else {
          _.each(self.plugins, function(value, key) {
            // load the plugins entry points.
            entryPoints.push(value.entryPoint);
            loadingName.innerHTML = key;
            pluginNames.push(key);
            // create plugin sandbox
            self.sandbox(key, value.entryPoint, function() {
              loadedPlugins++;
              loadingBar.style.width = loadedPlugins * 100 / pluginCount + '%';

              if(loadedPlugins === pluginCount) {
                // all plugins are loaded.
                window.alienLoadingFile = undefined;
                deferred.resolve();
              }
            });
          });
        }
      });
      return deferred;
    },
    sandbox: function(pluginName, entryPoint, callback) {
      // create a specific require configuration for this plugin and specifies the base url
      var config = {
        context: pluginName
      };

      // create a sandbox context for the plugin
      var sandbox = requirejs.config(config);
      var parentContext = requirejs.s.contexts._;
      var context = requirejs.s.contexts[pluginName];
      var namespace = _.camelCase(pluginName);

      context.localDefined = {};
      context.localDefined[entryPoint] = {};
      window[namespace] = { // register the plugin namespace to define elements in the right context
        define: function(name, deps, callback) {
          // same define function as requirejs. We just enforce the context to be the plugin's one.
          if (typeof name !== 'string') {
            callback = deps;
            deps = name;
            name = null;
          }

          if (!_.isArray(deps)) {
            callback = deps;
            deps = null;
          }

          if (!deps && _.isFunction(callback)) {
            deps = [];
            if (callback.length) {
              callback
                .toString()
                .replace(commentRegExp, '')
                .replace(cjsRequireRegExp, function (match, dep) {
                    deps.push(dep);
                  });
              deps = (callback.length === 1 ? ['require'] : ['require', 'exports', 'module']).concat(deps);
            }
          }

          // console.log('define module', context, name, deps, callback);
          // that's the difference with require js function...
          context.defQueue.push([name, deps, callback]);
          context.localDefined[name] = {};
        }
      };
      window[namespace].define.amd = define.amd;

      // Configure plugin to benefits from alien4cloud native dependencies.
      _.each(parentContext.defined, function(value, key) {
        context.defQueue.push([ key, [], function() { return value; }]);
      });

      // Loading plugin in it's sandbox
      sandbox([entryPoint], function() {
        // require the plugin module from the sandbox
        sandbox([pluginName], function(){
          callback();
        });
      });
    },
    base: function(pluginName) {
      if(_.defined(this.plugins[pluginName])) {
        return this.plugins[pluginName].base;
      }
      return ''; // the plugin has no defined base
    },

    //register a translation file
    registerTranslations: function(prefix, suffix){
      this.registeredTranlations.push({
        prefix: prefix,
        suffix: suffix ||'.json'
      });
    }
  };
});
