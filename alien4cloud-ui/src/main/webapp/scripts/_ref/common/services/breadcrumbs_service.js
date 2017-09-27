// Simplify the generation of crud resources for alien
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common').factory('breadcrumbsService',
    ['$resource', '$rootScope', '$state',
      function ($resource, $rootScope, $state) {
        var configByPath = {};
        var items = [];
        var stateMappings = {};

        var buildPaths = function (stateName) {
          var paths = [];
          var currentPath;
          var split = stateName.split('.');
          _.each(split, function (pathElement) {
            if (_.defined(currentPath)) {
              currentPath += '.';
            } else {
              currentPath = '';
            }
            currentPath += pathElement;
            paths.push(currentPath);
          });
          return paths;
        };

        var buildBreadcrumbs = function (stateName) {
          items = [];
          var paths = buildPaths(stateName);

          for (var i = 0; i < paths.length; i++) {
            var path = paths[i];

            var config = configByPath[path];
            if (_.defined(config) && _.defined(config.text)) {
              items.push({
                state: config.state,
                text: config.text,
                onClick: config.onClick
              });
            }
          }
          $rootScope.$broadcast('breadcrumbsUpdated');
        };

        var processStateMapping = function(stateName) {
          _.each(stateMappings, function(value, key){
            if(stateName.startsWith(key)){
              stateName = stateName.replace(key, value);
              return;
            }
          });

          return stateName;
        };

        $rootScope.$on('$stateChangeSuccess',
          function (event, toState, toParams, fromState, fromParams) {
            var stateName = toState.name;
            if (_.defined(_.get(toState, 'breadcrumbs.state'))) {
              stateName = toState.breadcrumbs.state;
            }
            stateName = processStateMapping(stateName);
            buildBreadcrumbs(stateName);
          });

        return {
          putConfig: function (config) {
            if(_.undefined(config.onClick)) {
              config.onClick = function() {
                $state.go(config.state);
              };
            }
            var stateName = config.state;
            configByPath[stateName] = config;
            buildBreadcrumbs(stateName);
          },
          getItems: function() {
            return items;
          },
          registerMapping: function(from, to) {
            stateMappings[from] = to;
          }
        };
      }
    ]);
});
