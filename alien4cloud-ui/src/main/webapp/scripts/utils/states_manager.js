/*
Route manager allow to configure states and their routes from different modules. It is based on angular-ui state configuration.

For routes that have children you can specify a layout, we will inject the list of child states elements with potential roles or resource roles.
Resource roles will be applied in the context of a role based resource that is the latest role based resource loaded in the hierarchy.

{
  url: 'state url',
  templateUrl: 'url of the template for the route (this may be a layout tempalte)',
  controller: 'name of the angular js controller to use for the route',
  resolve: {} resolve definition.
  menu: {
    id: '' id to associate to the clickable element allowing to reach the state in case of layout.
    text: '' text to be displayed (this can be a translation key actually).
    icon: '' font-awsome icon to be used in layouts that leverage icons (menus etc.)
    roles: [] array of roles required to see the menu element
  }
}
*/
define(function (require) {
  'use strict';

  var _ = require('lodash');

  function StatesManager() {
    this.states = {};
    this.stateForward = {};
  }

  StatesManager.prototype = {
    /** Add a route for a given state. */
    state: function(state, route) {
      var existingRoute = _.get(this.states, state, null);
      if(existingRoute === null) {
        // set the route for the given state
        _.set(this.states, state, {routeConfiguration: route});
      } else {
        // the existing route may be route definition or may have be created because of child states.
        if(_.has(existingRoute, 'routeConfiguration')) {
          console.warn('Warning, a route configuration will be overrided for state <' + state + '> you may have plugin conflicts.');
        }
        existingRoute.routeConfiguration = route;
      }
    },
    /** Merge a given route into an exising one and override only specified elements. */
    merge: function(state, route) {
      var existingRoute = _.get(this.states, state, null);
      if(existingRoute === null) {
        // set the route for the given state
        _.set(this.states, state, {routeConfiguration: route});
      } else {
        // the existing route may be route definition or may have be created because of child states.
        if(_.has(existingRoute, 'routeConfiguration')) {
          console.warn('Merging route configuration with existing.');
        }
        _.merge(existingRoute.routeConfiguration, route);
      }
    },
    /**
    * Configure the forwarding for a state to a target state.
    * Example .forward('admin', 'admin.home')
    */
    forward: function(state, stateforward) {
      this.stateForward[state] = stateforward;
    },
    config: function($stateProvider) {
      // configure the state provider with the states
      this.register($stateProvider, this.states, '');
    },
    register: function($stateProvider, states, prefix) {
      var self = this;
      _.forOwn(states, function(value, key) {
        var stateName = prefix + key;
        var stateConfig = _.omit(value.routeConfiguration, ['menu']);
        // resolve the list of child states in the route configuration so it is available to eventual layouts
        var childStates = _.omit(value, ['routeConfiguration']);
        var menu = self.menu(childStates);
        if(_.defined(menu) && !_.isEmpty(menu)) {
          _.set(stateConfig, 'resolve.menu', function() { return menu; });
          if(_.undefined(stateConfig.resolve.context)){
            _.set(stateConfig, 'resolve.context', function() { return {}; });
          }
        }

        $stateProvider.state(stateName, stateConfig);

        // register child states
        self.register($stateProvider, childStates, prefix + key + '.');
      });
    },
    rootMenu: function() {
      return this.menu(this.states);
    },
    menu: function(states) {
      var menu = [];
      _.forOwn(states, function(value) {
        if(_.has(value, 'routeConfiguration.menu')) {
          var menuItem = value.routeConfiguration.menu;
          menu.push(menuItem);
        }
      });
      menu = _.sortBy(menu, 'priority');
      return menu;
    }
  };

  return new StatesManager();
});
