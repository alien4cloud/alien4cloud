// Simplify the generation of crud resources for alien
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common').factory('breadcrumbsService',
    ['$resource', '$rootScope',
      function ($resource, $rootScope) {
        var configByPath = {};
        var items = [];

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

        $rootScope.$on('$stateChangeSuccess',
          function (event, toState, toParams, fromState, fromParams) {
            buildBreadcrumbs(toState.name);
          });

        var getItems = function () {
          return items;
        };

        var putConfig = function (state) {
          var stateName = state.state;
          configByPath[stateName] = state;
          buildBreadcrumbs(stateName);
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
        }

        return {
          putConfig: putConfig,
          getItems: getItems
        };

      }
    ]);
});
