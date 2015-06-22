// Helper object to manage modules
define(function (require) {
  'use strict';

  var angular = require('angular');
  var _ = require('lodash');

  function ModulesManager() {
    this.modules = {};
  }

  ModulesManager.prototype = {
    /**
    * Get an angular module.
    *
    * @param moduleName Name of the angular module to get.
    * @param requires Array of requirements/dependencies for the module.
    */
    get: function(moduleName, requires) {
      var module = this.modules[moduleName];

      if (module === void 0) {
        // create the module
        module = this.modules[moduleName] = angular.module(moduleName, []);
      }

      if (requires) {
        // update requires list with possibly new requirements
        module.requires = _.union(module.requires, requires);
      }

      return module;
    },

    /**
    * Link loaded modules to the given module.
    *
    * @param module The module in which to push all other modules as requirements.
    */
    link: function(module) {
      module.requires = _.union(module.requires, _.keys(this.modules));
    }
  };

  return new ModulesManager();
});
